package com.mathtowerdefense.tests;

import com.mathtowerdefense.api.FishGameAPI;
import com.mathtowerdefense.api.LocalQuestionGenerator;
import com.mathtowerdefense.api.Question;
import com.mathtowerdefense.authentication.LoginService;
import com.mathtowerdefense.authentication.PasswordHasher;
import com.mathtowerdefense.authentication.RegisterService;
import com.mathtowerdefense.engine.CollisionManager;
import com.mathtowerdefense.engine.GameConfig;
import com.mathtowerdefense.engine.GameEngine;
import com.mathtowerdefense.entities.BasicTower;
import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.EnemyType;
import com.mathtowerdefense.entities.MathBoostTower;
import com.mathtowerdefense.entities.Player;
import com.mathtowerdefense.entities.Projectile;
import com.mathtowerdefense.entities.Tower;
import com.mathtowerdefense.entities.TowerType;
import com.mathtowerdefense.managers.LevelManager;
import com.mathtowerdefense.managers.ScoreManager;
import com.mathtowerdefense.managers.WaveManager;
import com.mathtowerdefense.models.PlayerStats;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Self-contained test runner (no external test framework required).
 *
 * Run:   java -cp bin com.mathtowerdefense.tests.TestRunner
 * Live:  java -cp bin com.mathtowerdefense.tests.TestRunner --live
 *        (--live additionally performs ONE real Fish Game API call)
 *
 * Exit code 0 = all tests passed, 1 = at least one failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testRegistrationValidation();
        testLoginValidation();
        testPasswordHashing();
        testPlayerClampsAndAccuracy();
        testScoreManager();
        testComboMilestones();
        testLevelManager();
        testEnemyMovement();
        testCollision();
        testTowerTargeting();
        testTowerUpgrades();
        testMathBoostTowerHook();
        testWaveProgression();
        testAnswerValidation();
        testLocalGenerator();
        testApiJsonParsing();
        testPlacementValidation();
        testGameSimulation();
        testDatabaseFailureHandling();
        testSoundSynth();

        if (args.length > 0 && "--live".equals(args[0])) {
            liveApiSmokeTest();
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("TESTS: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // tiny assertion helpers
    // ------------------------------------------------------------------

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void checkEquals(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name + " - expected <" + expected
                    + "> but was <" + actual + ">");
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
    // ------------------------------------------------------------------
    // tests
    // ------------------------------------------------------------------

    private static void testRegistrationValidation() {
        section("Registration validation");
        check("empty fields rejected",
                RegisterService.validate("", "", "", "") != null);
        check("null fields rejected",
                RegisterService.validate(null, null, null, null) != null);
        check("short username rejected",
                RegisterService.validate("ab", "a@b.com", "secret1", "secret1") != null);
        check("invalid characters in username rejected",
                RegisterService.validate("bad name!", "a@b.com", "secret1", "secret1") != null);
        check("invalid email rejected",
                RegisterService.validate("player1", "not-an-email", "secret1", "secret1") != null);
        check("short password rejected",
                RegisterService.validate("player1", "a@b.com", "abc", "abc") != null);
        check("password mismatch rejected",
                RegisterService.validate("player1", "a@b.com", "secret1", "secret2") != null);
        check("valid input accepted",
                RegisterService.validate("player1", "a@b.com", "secret1", "secret1") == null);
    }

    private static void testLoginValidation() {
        section("Login validation");
        check("empty username rejected", LoginService.validate("", "x") != null);
        check("empty password rejected", LoginService.validate("user", "") != null);
        check("null username rejected", LoginService.validate(null, "x") != null);
        check("valid input accepted", LoginService.validate("player", "secret1") == null);
    }

    private static void testPasswordHashing() {
        section("Password hashing (PBKDF2)");
        PasswordHasher hasher = new PasswordHasher();
        String stored = hasher.hash("Secret123!");
        check("hash is not plain text", !stored.contains("Secret123!"));
        check("hash has the documented format", stored.startsWith("pbkdf2$"));
        check("correct password verifies", hasher.verify("Secret123!", stored));
        check("wrong password rejected", !hasher.verify("secret123!", stored));
        check("malformed stored hash rejected", !hasher.verify("x", "garbage"));
        check("null password rejected", !hasher.verify(null, stored));
        check("same password hashes differently (random salt)",
                !stored.equals(hasher.hash("Secret123!")));
    }

    private static void testPlayerClampsAndAccuracy() {
        section("Player clamps & accuracy");
        Player p = new Player("Tester", 300, 100);
        check("spend more than owned fails", !p.spendCoins(500));
        check("coins unchanged after failed spend", p.getCoins() == 300);
        check("valid spend works", p.spendCoins(100) && p.getCoins() == 200);
        p.addCoins(-1000);
        check("coins never negative", p.getCoins() == 0);
        p.addScore(-999);
        check("score never negative", p.getScore() == 0);
        p.setHealth(-50);
        check("health clamped to 0", p.getHealth() == 0);
        check("accuracy with 0 answers is 0", PlayerStats.empty(1).getAccuracy() == 0.0);
        p.recordAnswer(true);
        p.recordAnswer(true);
        p.recordAnswer(false);
        checkEquals("player accuracy 2/3", 66.7, p.getAccuracy());
    }

    private static void testScoreManager() {
        section("Score manager (spec section 22)");
        Player p = new Player("S", 100, 100);
        ScoreManager.awardEnemyKill(p, false);
        checkEquals("enemy kill +100", 100, p.getScore());
        ScoreManager.awardEnemyKill(p, true);
        checkEquals("boss kill +1000", 1100, p.getScore());
        ScoreManager.awardWaveCompleted(p);
        checkEquals("wave +500", 1600, p.getScore());
        ScoreManager.penaliseWrongAnswer(p);
        checkEquals("wrong -25", 1575, p.getScore());
        p.addScore(-100000);
        checkEquals("score clamped at 0", 0, p.getScore());
        checkEquals("accuracy zero-guard", 0.0, ScoreManager.accuracy(0, 0));
        checkEquals("accuracy 5 correct / 5 incorrect", 50.0, ScoreManager.accuracy(5, 5));
        checkEquals("accuracy 1 correct / 2 incorrect", 33.3, ScoreManager.accuracy(1, 2));
        checkEquals("easy difficulty health mult", 0.8,
                ScoreManager.difficultyHealthMultiplier(GameConfig.DIFFICULTY_EASY));
    }

    private static void testComboMilestones() {
        section("Combo milestones");
        checkEquals("combo 3 bonus", 100, ScoreManager.comboBonusFor(3));
        checkEquals("combo 5 bonus", 250, ScoreManager.comboBonusFor(5));
        checkEquals("combo 10 bonus", 600, ScoreManager.comboBonusFor(10));
        checkEquals("combo 4 no bonus", 0, ScoreManager.comboBonusFor(4));

        Player p = new Player("C", 100, 100);
        for (int i = 0; i < 3; i++) {
            p.recordAnswer(true);
            ScoreManager.awardCorrectAnswer(p);
        }
        checkEquals("3 correct: 3x50 + 100 milestone", 250, p.getScore());
        p.recordAnswer(false);
        checkEquals("wrong answer resets combo", 0, p.getCombo());
    }

    private static void testLevelManager() {
        section("Level manager");
        Player p = new Player("L", 100, 100);
        checkEquals("level starts at 1", 1, p.getLevel());
        int gained = LevelManager.grantExperience(p, LevelManager.totalXpFor(2));
        checkEquals("reaching lvl2 threshold grants 1 level", 1, gained);
        checkEquals("now level 2", 2, p.getLevel());
        check("totalXpFor(1) is 0", LevelManager.totalXpFor(1) == 0);
        check("xp required grows with level",
                LevelManager.xpRequiredFor(3) > LevelManager.xpRequiredFor(2));
        checkEquals("negative xp ignored", 0, LevelManager.grantExperience(p, -50));
    }
    private static void testEnemyMovement() {
        section("Enemy movement along waypoints");
        List<Point2D.Double> path = List.of(
                new Point2D.Double(0, 0),
                new Point2D.Double(100, 0));
        Enemy e = new Enemy(EnemyType.NORMAL, 0, 0, 1.0, 1.0, path);
        check("enemy starts alive", e.isAlive());
        check("normal enemy health matches type", e.getMaxHealth() == EnemyType.NORMAL.getBaseHealth());

        e.update(0.5); // 55 px/s * 0.5 s = 27.5 px
        checkEquals("moved 27.5 px", 27.5, Math.round(e.getX() * 10.0) / 10.0);
        check("not at base yet", !e.hasReachedBase());

        e.update(0.5);
        boolean reached = e.update(1.0);
        check("reports reaching the base", reached);
        check("reachedBase flag set", e.hasReachedBase());

        // Damage / death
        Enemy e2 = new Enemy(EnemyType.FAST, 0, 0, 1.0, 1.0, path);
        check("first hit does not kill", !e2.takeDamage(10));
        check("lethal hit reports kill", e2.takeDamage(EnemyType.FAST.getBaseHealth()));
        check("dead enemy not alive", !e2.isAlive());
        check("further damage ignored", !e2.takeDamage(10));

        Enemy e3 = new Enemy(EnemyType.NORMAL, 0, 0, 1.0, 1.0, path);
        e3.expire();
        check("expired enemy cannot be killed for rewards", !e3.takeDamage(1000));
    }

    private static void testCollision() {
        section("Collision / projectiles");
        List<Point2D.Double> path = List.of(
                new Point2D.Double(0, 0),
                new Point2D.Double(500, 0));
        Enemy target = new Enemy(EnemyType.NORMAL, 100, 0, 1.0, 1.0, path);
        int hpBefore = target.getHealth();

        Projectile p = new Projectile(100, 5, target, 20); // 5 px away = impact range
        boolean hit = p.update(0.1);
        check("point-blank projectile hits", hit);
        check("projectile deactivated after hit", !p.isActive());
        checkEquals("damage applied", hpBefore - 20, target.getHealth());

        // Out of range: projectile flies toward the target.
        Enemy far = new Enemy(EnemyType.NORMAL, 300, 0, 1.0, 1.0, path);
        Projectile fly = new Projectile(0, 0, far, 20);
        check("distant projectile does not hit yet", !fly.update(0.1));
        check("distant projectile still active", fly.isActive());
        check("moved toward target", fly.getX() > 0);

        // Target died -> projectile fizzles.
        Enemy dead = new Enemy(EnemyType.NORMAL, 100, 0, 1.0, 1.0, path);
        dead.expire();
        Projectile fizzle = new Projectile(100, 5, dead, 20);
        check("shot at dead target fizzles", !fizzle.update(0.1) && !fizzle.isActive());

        check("applyHit kills at lethal damage",
                CollisionManager.applyHit(target, target.getHealth() + 10));
        check("withinRange true", CollisionManager.withinRange(0, 0, 3, 4, 5));
        check("withinRange false", !CollisionManager.withinRange(0, 0, 3, 4, 4.9));
    }

    private static void testTowerTargeting() {
        section("Tower targeting (furthest along the path)");
        List<Point2D.Double> path = List.of(
                new Point2D.Double(0, 0),
                new Point2D.Double(1000, 0));
        Tower tower = new BasicTower(0, 0);

        Enemy near = new Enemy(EnemyType.NORMAL, 0, 0, 1.0, 1.0, path);   // travelled 0
        Enemy far = new Enemy(EnemyType.NORMAL, 0, 0, 1.0, 1.0, path);
        far.update(1.0);                                                   // travelled 55

        Enemy chosen = tower.findTarget(List.of(near, far));
        check("picks the enemy furthest along", chosen == far);
        check("null when nothing in range",
                tower.findTarget(List.of()) == null);

        Enemy outOfRange = new Enemy(EnemyType.NORMAL, 9999, 9999, 1.0, 1.0, path);
        check("ignores enemies outside range", tower.findTarget(List.of(outOfRange)) == null);
    }

    private static void testTowerUpgrades() {
        section("Tower upgrades (10 / 20 / 35 / 55)");
        Tower t = new BasicTower(0, 0);
        checkEquals("level 1 damage", 10, t.getDamage());
        checkEquals("level 2 damage", 20, t.upgrade() ? t.getDamage() : -1);
        checkEquals("level 3 damage", 35, t.upgrade() ? t.getDamage() : -1);
        checkEquals("level 4 damage", 55, t.upgrade() ? t.getDamage() : -1);
        check("max level reached", t.isMaxLevel());
        check("cannot upgrade past max", !t.upgrade());
        checkEquals("no upgrade cost at max", -1, t.getUpgradeCost());

        Tower t2 = new BasicTower(0, 0);
        int c1 = t2.getUpgradeCost();
        t2.upgrade();
        int c2 = t2.getUpgradeCost();
        t2.upgrade();
        int c3 = t2.getUpgradeCost();
        check("upgrade cost increases each level", c1 < c2 && c2 < c3);
        checkEquals("first upgrade cost 60 for 100-cost tower", 60, c1);
    }

    private static void testMathBoostTowerHook() {
        section("Math Boost tower (polymorphic hook)");
        Player p = new Player("M", 100, 100);
        Tower basic = new BasicTower(0, 0);
        Tower boost = new MathBoostTower(0, 50);

        check("basic tower gives no bonus", basic.onCorrectAnswer(p) == null);
        int coinsBefore = p.getCoins();
        int scoreBefore = p.getScore();
        String msg = boost.onCorrectAnswer(p);
        check("math boost announces its bonus", msg != null && msg.contains("15"));
        checkEquals("math boost grants coins", coinsBefore + 15, p.getCoins());
        checkEquals("math boost grants score", scoreBefore + 25, p.getScore());
        check("tower types available", TowerType.values().length == 4);
    }
    private static void testWaveProgression() {
        section("Wave progression (spec section 11)");
        WaveManager wm = new WaveManager();
        checkEquals("total waves", 10, wm.getTotalWaves());
        checkEquals("wave 1 has 5 enemies", 5, wm.waveAt(0).getEnemyCount());
        checkEquals("wave 2 has 7 enemies", 7, wm.waveAt(1).getEnemyCount());
        checkEquals("wave 3 has 10 enemies", 10, wm.waveAt(2).getEnemyCount());
        checkEquals("wave 4 has 12 enemies", 12, wm.waveAt(3).getEnemyCount());
        check("wave 5 is a boss wave", wm.waveAt(4).isBossWave());
        check("wave 10 is a boss wave", wm.waveAt(9).isBossWave());
        check("health grows every wave",
                wm.waveAt(1).getHealthMultiplier() > wm.waveAt(0).getHealthMultiplier());
        check("spawn interval shrinks every wave",
                wm.waveAt(1).getSpawnIntervalMs() < wm.waveAt(0).getSpawnIntervalMs());

        wm.startCampaign();
        checkEquals("campaign starts at wave 1", 1, wm.getCurrentWaveNumber());
        check("next wave exists", wm.hasNextWave());

        int spawned = 0;
        // Spawns are due immediately (timer 0) and after each interval.
        while (wm.pollSpawn(5.0) != null) {
            spawned++;
        }
        checkEquals("wave 1 fully spawned", 5, spawned);
        check("wave 1 fully spawned flag", wm.isCurrentWaveFullySpawned());
        check("not cleared while enemies alive", !wm.checkWaveCleared(3));
        check("cleared once empty", wm.checkWaveCleared(0));
        check("cleared only once", !wm.checkWaveCleared(0));

        check("advances to wave 2", wm.startNextWave());
        checkEquals("wave number now 2", 2, wm.getCurrentWaveNumber());

        // Run to the end of the campaign.
        while (wm.hasNextWave()) {
            wm.startNextWave();
        }
        check("no further wave after last", !wm.startNextWave());
        checkEquals("final wave number", 10, wm.getCurrentWaveNumber());
    }

    private static void testAnswerValidation() {
        section("Math answer validation");
        Question apiStyle = new Question("How many fish are in the picture?", "9",
                Question.Source.API, null, "https://example.com/q.png");
        check("exact answer correct", apiStyle.isCorrect("9"));
        check("whitespace tolerated", apiStyle.isCorrect("  9 "));
        check("leading zero tolerated", apiStyle.isCorrect("09"));
        check("wrong answer incorrect", !apiStyle.isCorrect("8"));
        check("empty input incorrect", !apiStyle.isCorrect(""));
        check("null input incorrect", !apiStyle.isCorrect(null));
        check("text input incorrect", !apiStyle.isCorrect("nine"));
        check("question is API-sourced", !apiStyle.isFallback());

        Question local = new Question("15 + 27 = ?", "42",
                Question.Source.LOCAL_FALLBACK, null, null);
        check("fallback question marked as fallback", local.isFallback());
        check("fallback arithmetic correct", local.isCorrect("42"));
    }

    private static void testLocalGenerator() {
        section("Offline fallback generator");
        LocalQuestionGenerator gen = new LocalQuestionGenerator();
        boolean allValid = true;
        boolean anyNonArithmetic = false;
        for (int i = 0; i < 50; i++) {
            Question q = gen.generate(1 + (i % 5));
            if (q == null || !q.isFallback() || q.getAnswer() == null
                    || !q.getPrompt().contains("=")) {
                allValid = false;
                continue;
            }
            try {
                Integer.parseInt(q.getAnswer());
            } catch (NumberFormatException e) {
                anyNonArithmetic = true;
            }
        }
        check("50 generated questions are well-formed", allValid);
        check("all answers are integers", !anyNonArithmetic);
    }

    private static void testApiJsonParsing() {
        section("Fish Game API response parsing");
        String realShape = "{ \"question\" : \"https://www.sanfoh.com/uob/fish/data/"
                + "ch_67cad8_233059_001x009.png\" , \"fish\" : 9, \"chest\" : 1 }";
        FishGameAPI.ParsedJson parsed = FishGameAPI.parseJson(realShape);
        check("documented response parses", parsed != null);
        if (parsed != null) {
            checkEquals("fish count", 9, parsed.fish);
            checkEquals("chest count", 1, parsed.chest);
            check("question is an image URL", parsed.isImageUrl());
        }

        String noFish = "{ \"question\" : \"https://example.com/x.png\" }";
        check("response without fish rejected", FishGameAPI.parseJson(noFish) == null);
        check("garbage rejected", FishGameAPI.parseJson("<html>error</html>") == null);
        check("null rejected", FishGameAPI.parseJson(null) == null);

        String base64Shape = "{ \"question\" : \"iVBORw0KGgo...\" , \"fish\" : 3, \"chest\" : 0 }";
        FishGameAPI.ParsedJson b64 = FishGameAPI.parseJson(base64Shape);
        check("base64 payload recognised", b64 != null && !b64.isImageUrl());
    }
    private static void testPlacementValidation() {
        section("Tower placement validation");
        GameEngine engine = new GameEngine("Tester", GameConfig.DIFFICULTY_NORMAL, null);
        checkEquals("fresh run starts in PLAYING",
                com.mathtowerdefense.engine.GameState.PLAYING, engine.getState());
        checkEquals("wave 1 running", 1, engine.getWaveNumber());

        check("valid grass position allowed",
                engine.canPlaceTower(TowerType.BASIC, 600, 200) == null);
        check("position on the path rejected",
                engine.canPlaceTower(TowerType.BASIC, 180, 150) != null);
        check("position off the map rejected",
                engine.canPlaceTower(TowerType.BASIC, 5, 5) != null);
        check("no tower type rejected",
                engine.canPlaceTower(null, 600, 200) != null);

        int coinsBefore = engine.getPlayer().getCoins();
        String error = engine.placeTower(TowerType.BASIC, 600, 200);
        check("placement succeeds on valid spot", error == null);
        checkEquals("coins deducted", coinsBefore - TowerType.BASIC.getCost(),
                engine.getPlayer().getCoins());
        checkEquals("tower added", 1, engine.getTowerManager().count());
        check("cannot build on top of another tower",
                engine.canPlaceTower(TowerType.BASIC, 600, 210) != null);

        engine.togglePause();
        checkEquals("pause transitions to PAUSED",
                com.mathtowerdefense.engine.GameState.PAUSED, engine.getState());
        engine.resume();
        checkEquals("resume returns to PLAYING",
                com.mathtowerdefense.engine.GameState.PLAYING, engine.getState());
    }

    /**
     * Headless full gameplay simulation (no GUI, no network): runs the real
     * engine for a bounded number of frames, answers the first challenge
     * correctly and checks that the world progresses (spawns, kills, waves).
     */
    private static void testGameSimulation() {
        section("Headless gameplay simulation");
        final boolean[] ended = { false };
        GameEngine engine = new GameEngine("SimPlayer", GameConfig.DIFFICULTY_EASY,
                new GameEngine.Listener() {
                    @Override
                    public void onNotice(String message, boolean positive) { }

                    @Override
                    public void onMathChallengeNeeded() { }

                    @Override
                    public void onGameEnded(boolean victory) {
                        ended[0] = true;
                    }
                });

        // Build two towers up front so the simulation can score kills.
        engine.getPlayer().addCoins(1000);
        check("towers placed for simulation",
                engine.placeTower(TowerType.POWER, 600, 200) == null
                && engine.placeTower(TowerType.RAPID, 560, 160) == null);

        boolean sawEnemies = false;
        boolean answeredOnce = false;
        int killsBefore = 0;
        int frames = 0;
        while (frames < 60 * 240 && !ended[0]) { // up to 4 simulated minutes
            engine.update(1.0 / 60.0);
            frames++;
            if (engine.getEnemyManager().countAlive() > 0) {
                sawEnemies = true;
            }
            killsBefore = engine.getPlayer().getEnemiesDefeated();
            if (engine.isChallengeOpen() && engine.getCurrentQuestion() == null
                    && !answeredOnce) {
                // Challenge open but (headless) fetch never delivered -> the
                // engine must guard itself with a local question. Fast-forward
                // the wait by repeated updates (the guard is 15 s).
            }
            if (engine.isChallengeOpen() && engine.getCurrentQuestion() != null
                    && !answeredOnce) {
                // Answer correctly using the known answer.
                GameEngine.AnswerResult r =
                        engine.submitAnswer(engine.getCurrentQuestion().getAnswer());
                if (r.answered && r.correct && engine.isMathPowerActive()) {
                    answeredOnce = true;
                }
            }
        }

        check("enemies spawned during simulation", sawEnemies);
        check("at least one challenge was answered correctly", answeredOnce);
        check("enemies were defeated", engine.getPlayer().getEnemiesDefeated() > killsBefore
                || engine.getPlayer().getEnemiesDefeated() > 0);
        check("player earned score", engine.getPlayer().getScore() > 0);
        check("simulation progressed beyond wave 1",
                engine.getWaveNumber() > 1 || engine.getPlayer().getWavesCompleted() > 0
                || ended[0]);
        System.out.println("        simulated " + (frames / 60) + "s: score="
                + engine.getPlayer().getScore() + ", kills="
                + engine.getPlayer().getEnemiesDefeated() + ", wave="
                + engine.getWaveNumber() + ", state=" + engine.getState());
    }

    /**
     * Database failure handling: no stack traces reach the caller, everything
     * degrades to friendly messages (the game stays playable as a guest).
     * Runs in seconds whether MySQL is present or not.
     */
    private static void testDatabaseFailureHandling() {
        section("Database failure handling");
        boolean available = false;
        boolean threw = false;
        try {
            available = com.mathtowerdefense.database.DatabaseConnection.isAvailable();
        } catch (RuntimeException e) {
            threw = true;
        }
        check("availability probe never throws", !threw);
        System.out.println("        MySQL reachable here: " + available);
        check("friendly unavailable message",
                com.mathtowerdefense.database.DatabaseConnection.unavailableMessage() != null
                && !com.mathtowerdefense.database.DatabaseConnection.unavailableMessage().isEmpty());

        LoginService.Result login =
                new LoginService().login("no_such_user_xyz", "secret123");
        check("login degrades gracefully without DB",
                !login.isSuccess() && login.getMessage() != null
                && !login.getMessage().isEmpty());
    }

    /**
     * Sound synthesis: every shipped .wav must decode as valid 8-bit mono
     * PCM with a correct WAV header, and recipes must be deterministic so the
     * live fallback sounds identical to the baked files.
     */
    private static void testSoundSynth() {
        section("Sound synthesis");
        boolean allKnown = true;
        for (com.mathtowerdefense.engine.SoundManager.Sound s
                : com.mathtowerdefense.engine.SoundManager.Sound.values()) {
            if (com.mathtowerdefense.engine.SoundSynth.specFor(s.getFileName()) == null) {
                allKnown = false;
            }
        }
        check("every sound enum value has a synth recipe", allKnown);
        check("unknown names have no recipe",
                com.mathtowerdefense.engine.SoundSynth.specFor("nope") == null);

        byte[] pcm1 = com.mathtowerdefense.engine.SoundSynth.renderNamed("correct");
        byte[] pcm2 = com.mathtowerdefense.engine.SoundSynth.renderNamed("correct");
        check("rendered PCM is non-empty", pcm1 != null && pcm1.length > 100);
        check("rendering is deterministic",
                pcm1 != null && pcm2 != null && java.util.Arrays.equals(pcm1, pcm2));

        byte[] wav = com.mathtowerdefense.engine.SoundSynth.toWav(pcm1);
        boolean headerOk = wav.length == pcm1.length + 44
                && wav[0] == 'R' && wav[1] == 'I' && wav[2] == 'F' && wav[3] == 'F'
                && wav[8] == 'W' && wav[9] == 'A' && wav[10] == 'V' && wav[11] == 'E'
                && wav[36] == 'd' && wav[37] == 'a' && wav[38] == 't' && wav[39] == 'a';
        check("WAV header is well-formed (RIFF/WAVE/data)", headerOk);

        // Compare against the baked files the game actually loads.
        boolean bakedOk = true;
        for (com.mathtowerdefense.engine.SoundManager.Sound s
                : com.mathtowerdefense.engine.SoundManager.Sound.values()) {
            byte[] file = com.mathtowerdefense.util.ResourceLoader
                    .loadBytes("sounds/" + s.getFileName() + ".wav");
            byte[] live = com.mathtowerdefense.engine.SoundSynth.toWav(
                    com.mathtowerdefense.engine.SoundSynth.renderNamed(s.getFileName()));
            if (file == null || live == null || !java.util.Arrays.equals(file, live)) {
                bakedOk = false;
            }
        }
        check("all baked .wav files match live synthesis byte-for-byte", bakedOk);

        byte[] music = com.mathtowerdefense.engine.SoundSynth.renderMusicLoop();
        check("music loop is ~8 s of audio",
                music != null && music.length
                        == com.mathtowerdefense.engine.SoundSynth.SAMPLE_RATE * 8);
    }

    /** Optional: ONE real call to the Fish Game API (--live). */
    private static void liveApiSmokeTest() {
        section("LIVE Fish Game API call (network required)");
        FishGameAPI api = new FishGameAPI();
        var response = api.fetchQuestion();
        if (response.isSuccess() && response.getQuestion() != null) {
            Question q = response.getQuestion();
            check("live question received", true);
            check("live question is API-sourced", !q.isFallback());
            check("live question has an image", q.getImage() != null);
            System.out.println("        prompt = " + q.getPrompt()
                    + " | answer = " + q.getAnswer());
        } else {
            System.out.println("        API unreachable: " + response.getErrorMessage());
            check("live API call (service reachable)", false);
        }
    }
}
