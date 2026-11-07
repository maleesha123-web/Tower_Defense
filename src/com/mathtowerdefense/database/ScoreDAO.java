package com.mathtowerdefense.database;

import com.mathtowerdefense.models.PlayerStats;
import com.mathtowerdefense.models.Score;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data access for the {@code scores} and {@code player_stats} tables.
 * Handles both persisting a finished game and reading lifetime statistics.
 */
public class ScoreDAO {

    /** Inserts one finished game (row of the {@code scores} table). */
    public boolean insertScore(Score score) throws SQLException {
        String sql = "INSERT INTO scores (user_id, score, level, wave, enemies_defeated) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, score.getUserId());
            ps.setInt(2, score.getScore());
            ps.setInt(3, score.getLevel());
            ps.setInt(4, score.getWave());
            ps.setInt(5, score.getEnemiesDefeated());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Updates (or creates) the lifetime statistics of a user after a game.
     * High scores use GREATEST() so only genuine improvements are stored.
     */
    public void updateStats(int userId, int score, int wave, int correct, int incorrect,
                            int enemiesDefeated) throws SQLException {
        String update = "UPDATE player_stats SET games_played = games_played + 1, "
                + "highest_score = GREATEST(highest_score, ?), highest_wave = GREATEST(highest_wave, ?), "
                + "correct_answers = correct_answers + ?, incorrect_answers = incorrect_answers + ?, "
                + "total_enemies_defeated = total_enemies_defeated + ? WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setInt(1, score);
            ps.setInt(2, wave);
            ps.setInt(3, correct);
            ps.setInt(4, incorrect);
            ps.setInt(5, enemiesDefeated);
            ps.setInt(6, userId);

            if (ps.executeUpdate() == 0) {
                // First game for this user -> create the stats row.
                String insert = "INSERT INTO player_stats (user_id, games_played, highest_score, "
                        + "highest_wave, correct_answers, incorrect_answers, total_enemies_defeated) "
                        + "VALUES (?, 1, ?, ?, ?, ?, ?)";
                try (PreparedStatement ins = c.prepareStatement(insert)) {
                    ins.setInt(1, userId);
                    ins.setInt(2, score);
                    ins.setInt(3, wave);
                    ins.setInt(4, correct);
                    ins.setInt(5, incorrect);
                    ins.setInt(6, enemiesDefeated);
                    ins.executeUpdate();
                }
            }
        }
    }

    /** Convenience: persists the score row and the statistics in one call. */
    public void saveGameResult(int userId, int score, int level, int wave,
                               int correct, int incorrect, int enemiesDefeated) throws SQLException {
        insertScore(Score.unpersisted(userId, null, score, level, wave, enemiesDefeated));
        updateStats(userId, score, wave, correct, incorrect, enemiesDefeated);
    }

    /** @return the player's lifetime statistics, or empty when no row exists yet */
    public Optional<PlayerStats> findStats(int userId) throws SQLException {
        String sql = "SELECT id, user_id, games_played, highest_score, highest_wave, "
                + "correct_answers, incorrect_answers, total_enemies_defeated "
                + "FROM player_stats WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerStats(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("games_played"),
                        rs.getInt("highest_score"),
                        rs.getInt("highest_wave"),
                        rs.getInt("correct_answers"),
                        rs.getInt("incorrect_answers"),
                        rs.getInt("total_enemies_defeated")));
            }
        }
    }

    /** @return a zeroed stats object when the row does not exist (new account) */
    public PlayerStats getStatsOrEmpty(int userId) throws SQLException {
        return findStats(userId).orElseGet(() -> PlayerStats.empty(userId));
    }
}
