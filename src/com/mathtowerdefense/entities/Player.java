package com.mathtowerdefense.entities;

/**
 * Runtime state of the player during a game session: score, coins, base
 * health mirror, progression and lifetime counters for the current run.
 *
 * Fully encapsulated: no mutable field is exposed, all mutations validate
 * their input (score/coins/health can never become negative).
 */
public class Player {

    private final String username;
    private int score;
    private int coins;
    private int health;
    private int maxHealth;
    private int level = 1;
    private int experience;

    // Current-run statistics (fed to the profile/leaderboard at game end).
    private int totalQuestions;
    private int correctAnswers;
    private int incorrectAnswers;
    private int enemiesDefeated;
    private int wavesCompleted;

    private int combo; // consecutive correct answers

    public Player(String username, int startingCoins, int startingHealth) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Player requires a username.");
        }
        this.username = username;
        this.coins = Math.max(0, startingCoins);
        this.maxHealth = Math.max(1, startingHealth);
        this.health = this.maxHealth;
    }

    // ------------------------------------------------------------------
    // Score / coins
    // ------------------------------------------------------------------

    /** Adds points (negative deltas are clamped at zero). */
    public void addScore(int delta) {
        this.score = Math.max(0, this.score + delta);
    }

    public int getScore() { return score; }

    public void addCoins(int delta) {
        this.coins = Math.max(0, this.coins + delta);
    }

    /**
     * Spends coins for a tower purchase/upgrade.
     *
     * @return true when the purchase succeeded; false (and unchanged coins)
     *         when the player cannot afford it
     */
    public boolean spendCoins(int amount) {
        if (amount < 0 || amount > coins) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public int getCoins() { return coins; }

    // ------------------------------------------------------------------
    // Health (mirror of the Base's health - single write path via engine)
    // ------------------------------------------------------------------

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public int getHealth() { return health; }

    public int getMaxHealth() { return maxHealth; }

    public double getHealthFraction() {
        return maxHealth <= 0 ? 0.0 : (double) health / maxHealth;
    }

    // ------------------------------------------------------------------
    // Progression
    // ------------------------------------------------------------------

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getLevel() { return level; }

    public void addExperience(int xp) {
        this.experience = Math.max(0, this.experience + Math.max(0, xp));
    }

    public int getExperience() { return experience; }

    // ------------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------------

    /** Records an answered math question and refreshes the combo counter. */
    public void recordAnswer(boolean correct) {
        totalQuestions++;
        if (correct) {
            correctAnswers++;
            combo++;
        } else {
            incorrectAnswers++;
            combo = 0;
        }
    }

    public int getTotalQuestions() { return totalQuestions; }

    public int getCorrectAnswers() { return correctAnswers; }

    public int getIncorrectAnswers() { return incorrectAnswers; }

    public int getCombo() { return combo; }

    public void incrementEnemiesDefeated() { enemiesDefeated++; }

    public int getEnemiesDefeated() { return enemiesDefeated; }

    public void incrementWavesCompleted() { wavesCompleted++; }

    public int getWavesCompleted() { return wavesCompleted; }

    /**
     * Answer accuracy in percent for the current run.
     *
     * @return 0.0 when nothing was answered yet (division-by-zero guard)
     */
    public double getAccuracy() {
        if (totalQuestions <= 0) {
            return 0.0;
        }
        return Math.round((correctAnswers * 100.0 / totalQuestions) * 10.0) / 10.0;
    }

    public String getUsername() { return username; }
}
