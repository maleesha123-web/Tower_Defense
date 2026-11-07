package com.mathtowerdefense.database;

import com.mathtowerdefense.models.Score;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only leaderboard queries (JOIN of {@code scores} and {@code users}),
 * ordered by highest score. Kept separate from ScoreDAO so each class has a
 * single, clear responsibility.
 */
public class LeaderboardDAO {

    /** Maximum rows ever returned - keeps the LIMIT bound safe. */
    private static final int MAX_LIMIT = 100;

    /**
     * @param limit maximum number of rows (clamped to 1..100)
     * @return leaderboard entries sorted by highest score first
     */
    public List<Score> getTopScores(int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String sql = "SELECT s.id, s.user_id, u.username, s.score, s.level, s.wave, "
                + "s.enemies_defeated, s.created_at "
                + "FROM scores s JOIN users u ON u.id = s.user_id "
                + "ORDER BY s.score DESC, s.created_at ASC LIMIT ?";
        List<Score> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    result.add(new Score(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("level"),
                            rs.getInt("wave"),
                            rs.getInt("enemies_defeated"),
                            ts == null ? null : ts.toLocalDateTime()));
                }
            }
        }
        return result;
    }
}
