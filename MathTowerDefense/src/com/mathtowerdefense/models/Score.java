package com.mathtowerdefense.models;

import java.time.LocalDateTime;

/**
 * One finished game on the leaderboard (row of the {@code scores} table).
 * The username is filled in by the leaderboard query via a JOIN so the GUI
 * never needs to touch SQL itself.
 */
public final class Score {

    private final int id;
    private final int userId;
    private final String username; // display name, may be null before the JOIN
    private final int score;
    private final int level;
    private final int wave;
    private final int enemiesDefeated;
    private final LocalDateTime createdAt;

    public Score(int id, int userId, String username, int score, int level, int wave,
                 int enemiesDefeated, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.level = level;
        this.wave = wave;
        this.enemiesDefeated = enemiesDefeated;
        this.createdAt = createdAt;
    }

    /** Convenience factory for a score that has not been persisted yet. */
    public static Score unpersisted(int userId, String username, int score, int level,
                                    int wave, int enemiesDefeated) {
        return new Score(0, userId, username, score, level, wave, enemiesDefeated, LocalDateTime.now());
    }

    public int getId() { return id; }

    public int getUserId() { return userId; }

    public String getUsername() { return username; }

    public int getScore() { return score; }

    public int getLevel() { return level; }

    public int getWave() { return wave; }

    public int getEnemiesDefeated() { return enemiesDefeated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
