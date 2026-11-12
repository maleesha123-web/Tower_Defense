package com.mathtowerdefense.models;

/**
 * Aggregated lifetime statistics for one user (row of the {@code player_stats}
 * table). Used by the profile screen.
 */
public final class PlayerStats {

    private final int id;
    private final int userId;
    private int gamesPlayed;
    private int highestScore;
    private int highestWave;
    private int correctAnswers;
    private int incorrectAnswers;
    private int totalEnemiesDefeated;

    public PlayerStats(int id, int userId, int gamesPlayed, int highestScore, int highestWave,
                       int correctAnswers, int incorrectAnswers, int totalEnemiesDefeated) {
        this.id = id;
        this.userId = userId;
        this.gamesPlayed = gamesPlayed;
        this.highestScore = highestScore;
        this.highestWave = highestWave;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.totalEnemiesDefeated = totalEnemiesDefeated;
    }

    /** Empty stats for a brand-new account (or a guest). */
    public static PlayerStats empty(int userId) {
        return new PlayerStats(0, userId, 0, 0, 0, 0, 0, 0);
    }

    public int getId() { return id; }

    public int getUserId() { return userId; }

    public int getGamesPlayed() { return gamesPlayed; }

    public int getHighestScore() { return highestScore; }

    public int getHighestWave() { return highestWave; }

    public int getCorrectAnswers() { return correctAnswers; }

    public int getIncorrectAnswers() { return incorrectAnswers; }

    public int getTotalEnemiesDefeated() { return totalEnemiesDefeated; }

    public int getTotalQuestions() { return correctAnswers + incorrectAnswers; }

    /**
     * Accuracy in percent.
     *
     * @return 0.0 when no question has been answered yet (division-by-zero guard),
     *         otherwise the rounded percentage 0..100
     */
    public double getAccuracy() {
        int total = getTotalQuestions();
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((correctAnswers * 100.0 / total) * 10.0) / 10.0;
    }
}
