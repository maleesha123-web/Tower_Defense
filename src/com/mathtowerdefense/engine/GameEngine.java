package com.mathtowerdefense.engine;

import com.mathtowerdefense.api.Question;
import com.mathtowerdefense.entities.Base;
import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.EnemyType;
import com.mathtowerdefense.entities.Player;
import com.mathtowerdefense.entities.Projectile;
import com.mathtowerdefense.entities.Tower;
import com.mathtowerdefense.entities.TowerType;
import com.mathtowerdefense.managers.EnemyManager;
import com.mathtowerdefense.managers.LevelManager;
import com.mathtowerdefense.managers.QuestionManager;
import com.mathtowerdefense.managers.ScoreManager;
import com.mathtowerdefense.managers.TowerManager;
import com.mathtowerdefense.managers.WaveManager;
import com.mathtowerdefense.api.LocalQuestionGenerator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The heart of the game: owns all managers/entities, applies the rules of
 * the game each frame and manages every {@link GameState} transition.
 *
 * The engine contains NO Swing code (except through the {@link Listener}
 * callbacks it invokes on the EDT) which keeps the simulation testable and
 * independent of the GUI layer.
 *
 * Update order per frame (state PLAYING):
 *   1. status timers (math power / weakened)
 *   2. wave spawning
 *   3. enemy movement + base leaks
 *   4. tower targeting & firing
 *   5. projectile collisions & kill rewards
 *   6. wave completion / intermission
 *   7. math-challenge scheduling
 */
public class GameEngine {

    // ------------------------------------------------------------------
    // Timing constants (seconds)
    // ------------------------------------------------------------------
    public static final double FIRST_CHALLENGE_DELAY = 10.0;
    public static final double CHALLENGE_INTERVAL = 15.0;
    public static final double CHALLENGE_TIME = 10.0;      // answer timer
    public static final double QUESTION_LOAD_TIMEOUT = 15.0; // fallback guard
    public static final double POWER_DURATION = 12.0;       // correct-answer buff
    public static final double WEAKEN_DURATION = 8.0;       // wrong-answer debuff
    public static final double INTERMISSION_SECONDS = 5.0;  // between waves

    // ------------------------------------------------------------------
    // Rewards (see ScoreManager for the score values)
    // ------------------------------------------------------------------
    public static final int CORRECT_ANSWER_COINS = 15;
    public static final int WRONG_ANSWER_COINS = 10;
    public static final int WRONG_ANSWER_BASE_DAMAGE = 5;
    public static final int WAVE_COMPLETED_COINS = 100;

    /** Callbacks the engine fires toward the GUI (always on the EDT). */
    public interface Listener {
        /** Transient toast message (positive = success styling). */
        void onNotice(String message, boolean positive);
        /** A challenge opened without a preloaded question - fetch one async. */
        void onMathChallengeNeeded();
        /** Terminal state reached; GUI saves the score and shows the screen. */
        void onGameEnded(boolean victory);
    }

    /** Rising/fading score text shown over the battlefield. */
    public static final class FloatingText {
        public final double x, y;
        public final String text;
        public final Color color;
        public double life;

        FloatingText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.life = 1.2;
        }
    }

    /** Short-lived kill particle. */
    public static final class Particle {
        public final Color color;
        public final int size;
        public double x, y, vx, vy;
        public double life;

        Particle(double x, double y, double vx, double vy, Color color, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.life = 0.6;
        }
    }

    /** Outcome of a submitted (or timed-out) math answer. */
    public static final class AnswerResult {
        public final boolean answered;
        public final boolean correct;
        public final boolean timeout;
        public final int scoreDelta;
        public final int coinsDelta;
        public final String message;

        private AnswerResult(boolean answered, boolean correct, boolean timeout,
                             int scoreDelta, int coinsDelta, String message) {
            this.answered = answered;
            this.correct = correct;
            this.timeout = timeout;
            this.scoreDelta = scoreDelta;
            this.coinsDelta = coinsDelta;
            this.message = message;
        }

        static AnswerResult ignored() {
            return new AnswerResult(false, false, false, 0, 0, "No active question.");
        }
    }
    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    private final String username;
    private final int difficulty;
    private final Listener listener;

    private final GameMap map;
    private Player player;
    private Base base;
    private EnemyManager enemyManager;
    private TowerManager towerManager;
    private WaveManager waveManager;
    private QuestionManager questionManager;
    private final CollisionManager collisionManager;

    private final List<Projectile> projectiles;
    private final List<FloatingText> floatingTexts;
    private final List<Particle> particles;

    private GameState state = GameState.MENU;

    // Math-challenge bookkeeping
    private Question currentQuestion;
    private String currentWarning;
    private double challengeTimeLeft;
    private double questionWait;           // loading guard while question == null
    private boolean challengeOpen;         // a challenge is on screen
    private Question preloadedQuestion;    // fetched ahead of time by the GUI
    private String preloadedWarning;
    private double challengeCountdown;

    private double intermission;           // seconds until the next wave
    private double powerTime;              // >0 : math power active
    private double weakenTime;             // >0 : weakened active
    private boolean ended;                 // game-over/victory fired once

    public GameEngine(String username, int difficulty, Listener listener) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("GameEngine requires a username.");
        }
        this.username = username;
        this.difficulty = difficulty;
        this.listener = listener;
        this.map = new GameMap();
        this.projectiles = new ArrayList<>();
        this.floatingTexts = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.collisionManager = new CollisionManager();
        startNewGame();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** (Re)initialises everything for a fresh run and enters PLAYING. */
    public final void startNewGame() {
        GameConfig cfg = GameConfig.getInstance();

        player = new Player(username, cfg.getStartingCoins(), cfg.getStartingBaseHealth());
        base = new Base(map.getBasePoint().x, map.getBasePoint().y, cfg.getStartingBaseHealth());
        enemyManager = new EnemyManager();
        towerManager = new TowerManager();
        waveManager = new WaveManager();
        questionManager = new QuestionManager();

        projectiles.clear();
        floatingTexts.clear();
        particles.clear();

        currentQuestion = null;
        currentWarning = null;
        preloadedQuestion = null;
        preloadedWarning = null;
        challengeOpen = false;
        challengeTimeLeft = 0;
        questionWait = 0;
        challengeCountdown = FIRST_CHALLENGE_DELAY;
        intermission = 0;
        powerTime = 0;
        weakenTime = 0;
        ended = false;

        waveManager.startCampaign();
        state = GameState.PLAYING;

        if (listener != null) {
            listener.onNotice("Wave 1 incoming - defend the base!", true);
        }
        SoundManager.getInstance().play(SoundManager.Sound.WAVE);
        SoundManager.getInstance().startMusic();
    }
    // ------------------------------------------------------------------
    // Frame update
    // ------------------------------------------------------------------

    /** Advances the simulation by {@code dt} seconds (no-op unless running). */
    public void update(double dt) {
        if (state != GameState.PLAYING && state != GameState.MATH_CHALLENGE) {
            return; // PAUSED / GAME_OVER / VICTORY / MENU freeze the world
        }

        if (state == GameState.PLAYING) {
            updateStatusTimers(dt);
            updateSpawning(dt);
            if (state == GameState.PLAYING) {
                updateEnemies(dt);
            }
            if (state == GameState.PLAYING) {
                updateTowersAndProjectiles(dt);
                updateWaveProgress(dt);
                updateChallengeSchedule(dt);
            }
        } else {
            // MATH_CHALLENGE: world frozen, only the question flow continues.
            updateChallengeFlow(dt);
        }

        updateEffects(dt);
    }

    private void updateStatusTimers(double dt) {
        if (powerTime > 0) {
            powerTime = Math.max(0, powerTime - dt);
        }
        if (weakenTime > 0) {
            weakenTime = Math.max(0, weakenTime - dt);
        }
        if (challengeCountdown > 0) {
            challengeCountdown = Math.max(0, challengeCountdown - dt);
        }
    }

    private void updateSpawning(double dt) {
        if (intermission > 0) {
            intermission = Math.max(0, intermission - dt);
            if (intermission == 0 && waveManager.hasNextWave()) {
                waveManager.startNextWave();
                notify("Wave " + waveManager.getCurrentWaveNumber() + " incoming!", true);
                SoundManager.getInstance().play(SoundManager.Sound.WAVE);
            }
            return; // no spawning between waves
        }
        EnemyType type = waveManager.pollSpawn(dt);
        if (type != null) {
            double diffHealth = ScoreManager.difficultyHealthMultiplier(difficulty);
            double diffSpeed = difficulty == GameConfig.DIFFICULTY_EASY ? 0.92
                    : difficulty == GameConfig.DIFFICULTY_HARD ? 1.12 : 1.0;
            var wave = waveManager.currentWave();
            enemyManager.spawn(type, map.getWaypoints(),
                    wave.getHealthMultiplier() * diffHealth,
                    wave.getSpeedMultiplier() * diffSpeed);
        }
    }

    private void updateEnemies(double dt) {
        List<Enemy> escaped = new ArrayList<>();
        enemyManager.update(dt, escaped);
        for (Enemy e : escaped) {
            e.expire(); // no ghost kills for enemies that already leaked
            int damage = e.getDamage();
            base.takeDamage(damage);
            player.setHealth(base.getHealth());
            notify("A " + e.getType().getDisplayName() + " enemy hit the base! -"
                    + damage + " HP", false);
            SoundManager.getInstance().play(SoundManager.Sound.LEAK);
            if (base.isDestroyed()) {
                endGame(false);
                return;
            }
        }
        enemyManager.removeInactive();
    }

    private void updateTowersAndProjectiles(double dt) {
        int fired = towerManager.update(dt, enemyManager.getEnemies(),
                getDamageMultiplier(), projectiles);
        if (fired > 0) {
            SoundManager.getInstance().play(SoundManager.Sound.ATTACK);
        }

        List<CollisionManager.Explosion> kills = collisionManager.update(projectiles, dt);
        for (CollisionManager.Explosion ex : kills) {
            Enemy dead = ex.killed;
            boolean boss = dead.isBoss();
            int points = ScoreManager.awardEnemyKill(player, boss);
            player.addCoins(dead.getReward());
            player.incrementEnemiesDefeated();
            int levels = LevelManager.grantExperience(player, boss ? 150 : 20);
            addFloatingText(dead.getX(), dead.getY() - dead.getRadius(),
                    "+" + points, boss ? new Color(255, 215, 0) : new Color(120, 255, 160));
            spawnBurst(dead);
            SoundManager.getInstance().play(SoundManager.Sound.HIT);
            if (levels > 0) {
                notify("LEVEL UP! Now level " + player.getLevel(), true);
            }
        }
        enemyManager.removeInactive();
    }

    private void updateWaveProgress(double dt) {
        if (intermission > 0) {
            return; // wave already cleared, waiting for the next one
        }
        if (waveManager.checkWaveCleared(enemyManager.countAlive())) {
            int waveNo = waveManager.getCurrentWaveNumber();
            player.incrementWavesCompleted();
            ScoreManager.awardWaveCompleted(player);
            player.addCoins(WAVE_COMPLETED_COINS);
            LevelManager.grantExperience(player, 100);
            SoundManager.getInstance().play(SoundManager.Sound.WAVE);

            if (!waveManager.hasNextWave()) {
                notify("All waves cleared!", true);
                endGame(true);
                return;
            }
            notify("Wave " + waveNo + " cleared! +" + ScoreManager.WAVE_COMPLETED
                    + " score, +" + WAVE_COMPLETED_COINS + " coins", true);
            intermission = INTERMISSION_SECONDS;
        }
    }

    private void updateChallengeSchedule(double dt) {
        if (challengeOpen || intermission > 0) {
            return;
        }
        if (challengeCountdown <= 0) {
            openChallenge();
        }
    }
    // ------------------------------------------------------------------
    // Math challenge flow
    // ------------------------------------------------------------------

    /** Opens a challenge: consumes a preloaded question or asks the GUI for one. */
    private void openChallenge() {
        challengeOpen = true;
        state = GameState.MATH_CHALLENGE;
        questionWait = 0;
        challengeTimeLeft = 0;
        SoundManager.getInstance().play(SoundManager.Sound.CHALLENGE);

        if (preloadedQuestion != null) {
            currentQuestion = preloadedQuestion;
            currentWarning = preloadedWarning;
            preloadedQuestion = null;
            preloadedWarning = null;
            challengeTimeLeft = CHALLENGE_TIME;
            if (currentWarning != null) {
                notify(currentWarning, false);
            }
        } else {
            currentQuestion = null;
            currentWarning = null;
            if (listener != null) {
                listener.onMathChallengeNeeded();
            }
        }
    }

    /** Question flow while the challenge is on screen (world frozen). */
    private void updateChallengeFlow(double dt) {
        if (!challengeOpen) {
            state = GameState.PLAYING;
            return;
        }
        if (currentQuestion == null) {
            questionWait += dt;
            if (questionWait >= QUESTION_LOAD_TIMEOUT) {
                // Absolute safety net: never hang on a stuck fetch.
                useLocalFallback("Question service did not respond in time.");
            }
            return;
        }
        challengeTimeLeft -= dt;
        if (challengeTimeLeft <= 0) {
            submitAnswer(null); // timeout = treated as incorrect (spec section 12)
        }
    }

    /**
     * Called by the GUI (EDT) when an async fetch finished.
     * If a challenge is waiting for a question, the timer starts now;
     * otherwise the question is kept preloaded for the next challenge.
     */
    public void deliverQuestion(Question question, String warning) {
        if (question == null) {
            return;
        }
        if (challengeOpen && currentQuestion == null) {
            currentQuestion = question;
            currentWarning = warning;
            questionWait = 0;
            challengeTimeLeft = CHALLENGE_TIME;
            if (warning != null) {
                notify(warning, false);
            }
        } else if (preloadedQuestion == null) {
            preloadedQuestion = question;
            preloadedWarning = warning;
        }
    }

    /** Forces an honest local question (clearly marked, never as API). */
    private void useLocalFallback(String reason) {
        int level = waveManager.getCurrentWaveNumber();
        Question local = new LocalQuestionGenerator().generate(Math.max(1, level));
        deliverQuestion(local, reason + " Using a local offline question.");
    }

    /**
     * Submits the answer for the open challenge.
     *
     * @param input player input, or {@code null} for a timeout
     * @return the outcome (ignored result when no challenge is open)
     */
    public AnswerResult submitAnswer(String input) {
        if (!challengeOpen || currentQuestion == null) {
            return AnswerResult.ignored();
        }
        boolean timeout = input == null;
        boolean correct = !timeout && currentQuestion.isCorrect(input);

        int scoreBefore = player.getScore();
        int coinsBefore = player.getCoins();
        String message;

        if (correct) {
            player.recordAnswer(true);
            int gained = ScoreManager.awardCorrectAnswer(player);
            player.addCoins(CORRECT_ANSWER_COINS);
            powerTime = POWER_DURATION;
            weakenTime = 0;
            String towerBonus = towerManager.notifyCorrectAnswer(player);
            LevelManager.grantExperience(player, 40);
            message = "Correct! +" + gained + " score, +" + CORRECT_ANSWER_COINS + " coins"
                    + (towerBonus != null ? " | " + towerBonus : "")
                    + " - Math Power active!";
            SoundManager.getInstance().play(SoundManager.Sound.CORRECT);
        } else {
            player.recordAnswer(false);
            ScoreManager.penaliseWrongAnswer(player);
            player.addCoins(-WRONG_ANSWER_COINS);
            base.takeDamage(WRONG_ANSWER_BASE_DAMAGE);
            player.setHealth(base.getHealth());
            weakenTime = WEAKEN_DURATION;
            powerTime = 0;
            message = timeout
                    ? "Time's up! -" + ScoreManager.WRONG_ANSWER + " score, -"
                      + WRONG_ANSWER_COINS + " coins, -" + WRONG_ANSWER_BASE_DAMAGE + " HP"
                    : "Wrong answer! -" + ScoreManager.WRONG_ANSWER + " score, -"
                      + WRONG_ANSWER_COINS + " coins, -" + WRONG_ANSWER_BASE_DAMAGE + " HP";
            SoundManager.getInstance().play(SoundManager.Sound.WRONG);
        }

        AnswerResult result = new AnswerResult(true, correct, timeout,
                player.getScore() - scoreBefore, player.getCoins() - coinsBefore, message);
        notify(message, correct);

        // Close the challenge and resume the world.
        currentQuestion = null;
        currentWarning = null;
        challengeOpen = false;
        state = GameState.PLAYING;
        challengeCountdown = CHALLENGE_INTERVAL;

        if (base.isDestroyed()) {
            endGame(false);
        }
        return result;
    }
    // ------------------------------------------------------------------
    // Effects & events
    // ------------------------------------------------------------------

    private void updateEffects(double dt) {
        floatingTexts.removeIf(t -> (t.life -= dt) <= 0);
        particles.removeIf(p -> {
            p.life -= dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 60 * dt; // slight gravity
            return p.life <= 0;
        });
    }

    private void addFloatingText(double x, double y, String text, Color color) {
        if (floatingTexts.size() < 40) {
            floatingTexts.add(new FloatingText(x, y, text, color));
        }
    }

    private void spawnBurst(Enemy dead) {
        Color c = dead.getType().getColor();
        for (int i = 0; i < 7; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 40 + Math.random() * 90;
            particles.add(new Particle(dead.getX(), dead.getY(),
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    c, 3 + (int) (Math.random() * 3)));
        }
        if (particles.size() > 300) {
            particles.remove(0);
        }
    }

    private void notify(String message, boolean positive) {
        if (listener != null && message != null) {
            listener.onNotice(message, positive);
        }
    }

    private void endGame(boolean victory) {
        if (ended) {
            return;
        }
        ended = true;
        state = victory ? GameState.VICTORY : GameState.GAME_OVER;
        SoundManager.getInstance().stopMusic();
        SoundManager.getInstance().play(victory
                ? SoundManager.Sound.VICTORY : SoundManager.Sound.GAME_OVER);
        if (listener != null) {
            listener.onGameEnded(victory);
        }
    }

    // ------------------------------------------------------------------
    // Player actions (called by the GUI)
    // ------------------------------------------------------------------

    /**
     * Validates a tower placement without spending coins.
     *
     * @return {@code null} when placement is allowed, otherwise a friendly reason
     */
    public String canPlaceTower(TowerType type, double x, double y) {
        if (state != GameState.PLAYING && state != GameState.MATH_CHALLENGE) {
            return "Towers can only be placed while the game is running.";
        }
        if (type == null) {
            return "Select a tower first.";
        }
        if (!map.isInsideMap(x, y, 30)) {
            return "Too close to the edge of the map.";
        }
        if (map.isOnPath(x, y, 34)) {
            return "Towers cannot be built on the enemy path.";
        }
        if (towerManager.isOccupied(x, y, 48)) {
            return "Too close to another tower.";
        }
        if (player.getCoins() < type.getCost()) {
            return "Not enough coins - " + type.getDisplayName() + " costs "
                    + type.getCost() + ".";
        }
        return null;
    }

    /**
     * Places a tower at the given map position.
     *
     * @return {@code null} on success, otherwise a friendly error message
     */
    public String placeTower(TowerType type, double x, double y) {
        String problem = canPlaceTower(type, x, y);
        if (problem != null) {
            return problem;
        }
        player.spendCoins(type.getCost());
        Tower tower = towerManager.createTower(type, x, y);
        towerManager.selectAt(x, y, 48);
        SoundManager.getInstance().play(SoundManager.Sound.BUILD);
        notify(type.getDisplayName() + " built (-" + type.getCost() + " coins)", true);
        return null;
    }

    /** Selects the tower nearest to a map click (within the pickup radius). */
    public Tower selectTowerAt(double x, double y) {
        return towerManager.selectAt(x, y, 34);
    }

    public void clearTowerSelection() {
        towerManager.clearSelection();
    }

    /**
     * Upgrades the selected tower when affordable.
     *
     * @return {@code null} on success, otherwise a friendly error message
     */
    public String upgradeSelectedTower() {
        Tower tower = towerManager.getSelected();
        if (tower == null) {
            return "Select a tower first.";
        }
        int cost = tower.getUpgradeCost();
        if (cost < 0) {
            return tower.getDisplayName() + " is already max level.";
        }
        if (!player.spendCoins(cost)) {
            return "Not enough coins - upgrade costs " + cost + ".";
        }
        tower.upgrade();
        SoundManager.getInstance().play(SoundManager.Sound.UPGRADE);
        notify(tower.getDisplayName() + " upgraded to level " + tower.getLevel(), true);
        return null;
    }

    /** Toggles between PLAYING and PAUSED. */
    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    /** Returns from PAUSED to PLAYING (no-op in other states). */
    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }
    // ------------------------------------------------------------------
    // Read access for the GUI / tests
    // ------------------------------------------------------------------

    public GameState getState() { return state; }

    public Player getPlayer() { return player; }

    public Base getBase() { return base; }

    public GameMap getMap() { return map; }

    public EnemyManager getEnemyManager() { return enemyManager; }

    public TowerManager getTowerManager() { return towerManager; }

    public WaveManager getWaveManager() { return waveManager; }

    public QuestionManager getQuestionManager() { return questionManager; }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public List<FloatingText> getFloatingTexts() {
        return Collections.unmodifiableList(floatingTexts);
    }

    public List<Particle> getParticles() {
        return Collections.unmodifiableList(particles);
    }

    public boolean isChallengeOpen() { return challengeOpen; }

    public Question getCurrentQuestion() { return currentQuestion; }

    /** Warning text for the current question (API failure notice), or null. */
    public String getCurrentWarning() { return currentWarning; }

    public double getChallengeTimeLeft() { return Math.max(0, challengeTimeLeft); }

    /** True while a challenge is open but the question has not arrived yet. */
    public boolean isQuestionLoading() {
        return challengeOpen && currentQuestion == null;
    }

    /** Seconds until the next challenge (0 while one is open). */
    public double getChallengeCountdown() { return challengeCountdown; }

    /** Seconds until the next wave starts (0 when no intermission). */
    public double getIntermission() { return intermission; }

    public double getPowerTimeLeft() { return powerTime; }

    public double getWeakenTimeLeft() { return weakenTime; }

    public boolean isMathPowerActive() { return powerTime > 0; }

    public boolean isWeakened() { return weakenTime > 0; }

    /**
     * Global damage multiplier from the math states:
     * 1.5x while Math Power is active, 0.5x while weakened, else 1.0x.
     */
    public double getDamageMultiplier() {
        if (powerTime > 0) {
            return 1.5;
        }
        if (weakenTime > 0) {
            return 0.5;
        }
        return 1.0;
    }

    public int getWaveNumber() {
        return waveManager.getCurrentWaveNumber();
    }

    public int getTotalWaves() {
        return waveManager.getTotalWaves();
    }

    public boolean isEnded() {
        return state == GameState.GAME_OVER || state == GameState.VICTORY;
    }

    public int getDifficulty() { return difficulty; }

    public String getUsername() { return username; }
}
