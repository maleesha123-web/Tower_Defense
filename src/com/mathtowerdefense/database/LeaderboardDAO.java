package com.mathtowerdefense.database;

import com.mathtowerdefense.models.Score;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read-only leaderboard queries from the {@code scores} collection in MongoDB,
 * ordered by highest score.
 */
public class LeaderboardDAO {

    /** Maximum rows ever returned - keeps the LIMIT bound safe. */
    private static final int MAX_LIMIT = 100;

    private MongoCollection<Document> collection() {
        return DatabaseConnection.getDatabase().getCollection("scores");
    }

    /**
     * @param limit maximum number of rows (clamped to 1..100)
     * @return leaderboard entries sorted by highest score first
     */
    public List<Score> getTopScores(int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Score> result = new ArrayList<>();
        try {
            for (Document doc : collection()
                    .find()
                    .sort(Sorts.descending("score"))
                    .limit(safeLimit)) {

                int id = doc.get("id") instanceof Number n ? n.intValue() : 0;
                int userId = doc.get("user_id") instanceof Number n ? n.intValue() : 0;
                String username = doc.getString("username");
                int score = doc.get("score") instanceof Number n ? n.intValue() : 0;
                int level = doc.get("level") instanceof Number n ? n.intValue() : 1;
                int wave = doc.get("wave") instanceof Number n ? n.intValue() : 0;
                int enemies = doc.get("enemies_defeated") instanceof Number n ? n.intValue() : 0;
                Date created = doc.getDate("created_at");

                result.add(new Score(
                        id,
                        userId,
                        username,
                        score,
                        level,
                        wave,
                        enemies,
                        DatabaseConnection.toLocalDateTime(created)));
            }
        } catch (Exception e) {
            throw new SQLException("MongoDB error loading leaderboard: " + e.getMessage(), e);
        }
        return result;
    }
}
