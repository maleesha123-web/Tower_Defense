package com.mathtowerdefense.database;

import com.mathtowerdefense.models.PlayerStats;
import com.mathtowerdefense.models.Score;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Data access for the {@code scores} and {@code player_stats} collections in MongoDB.
 * Handles both persisting a finished game and reading lifetime statistics.
 */
public class ScoreDAO {

    private MongoCollection<Document> scoresCollection() {
        return DatabaseConnection.getDatabase().getCollection("scores");
    }

    private MongoCollection<Document> statsCollection() {
        return DatabaseConnection.getDatabase().getCollection("player_stats");
    }

    private MongoCollection<Document> usersCollection() {
        return DatabaseConnection.getDatabase().getCollection("users");
    }

    /** Inserts one finished game (document in the {@code scores} collection). */
    public boolean insertScore(Score score) throws SQLException {
        try {
            int nextId = DatabaseConnection.getNextSequence("scores");

            String username = score.getUsername();
            if (username == null || username.isBlank()) {
                Document userDoc = usersCollection().find(Filters.eq("id", score.getUserId())).first();
                if (userDoc != null) {
                    username = userDoc.getString("username");
                }
            }
            if (username == null) {
                username = "Player";
            }

            Document doc = new Document("id", nextId)
                    .append("user_id", score.getUserId())
                    .append("username", username)
                    .append("score", score.getScore())
                    .append("level", score.getLevel())
                    .append("wave", score.getWave())
                    .append("enemies_defeated", score.getEnemiesDefeated())
                    .append("created_at", new Date());

            scoresCollection().insertOne(doc);
            return true;
        } catch (Exception e) {
            throw new SQLException("MongoDB error inserting score: " + e.getMessage(), e);
        }
    }

    /**
     * Updates (or creates) the lifetime statistics of a user after a game.
     * High scores only update on genuine improvements.
     */
    public void updateStats(int userId, int score, int wave, int correct, int incorrect,
                            int enemiesDefeated) throws SQLException {
        try {
            Document existing = statsCollection().find(Filters.eq("user_id", userId)).first();
            if (existing == null) {
                int nextId = DatabaseConnection.getNextSequence("player_stats");
                Document newStats = new Document("id", nextId)
                        .append("user_id", userId)
                        .append("games_played", 1)
                        .append("highest_score", score)
                        .append("highest_wave", wave)
                        .append("correct_answers", correct)
                        .append("incorrect_answers", incorrect)
                        .append("total_enemies_defeated", enemiesDefeated);
                statsCollection().insertOne(newStats);
            } else {
                int prevGames = getInt(existing, "games_played", 0);
                int prevScore = getInt(existing, "highest_score", 0);
                int prevWave = getInt(existing, "highest_wave", 0);
                int prevCorrect = getInt(existing, "correct_answers", 0);
                int prevIncorrect = getInt(existing, "incorrect_answers", 0);
                int prevEnemies = getInt(existing, "total_enemies_defeated", 0);

                Document updated = new Document("$set", new Document()
                        .append("games_played", prevGames + 1)
                        .append("highest_score", Math.max(prevScore, score))
                        .append("highest_wave", Math.max(prevWave, wave))
                        .append("correct_answers", prevCorrect + correct)
                        .append("incorrect_answers", prevIncorrect + incorrect)
                        .append("total_enemies_defeated", prevEnemies + enemiesDefeated));

                statsCollection().updateOne(Filters.eq("user_id", userId), updated);
            }
        } catch (Exception e) {
            throw new SQLException("MongoDB error updating player stats: " + e.getMessage(), e);
        }
    }

    /** Convenience: persists the score document and the statistics in one call. */
    public void saveGameResult(int userId, int score, int level, int wave,
                               int correct, int incorrect, int enemiesDefeated) throws SQLException {
        insertScore(Score.unpersisted(userId, null, score, level, wave, enemiesDefeated));
        updateStats(userId, score, wave, correct, incorrect, enemiesDefeated);
    }

    /** @return the player's lifetime statistics, or empty when no document exists yet */
    public Optional<PlayerStats> findStats(int userId) throws SQLException {
        try {
            Document doc = statsCollection().find(Filters.eq("user_id", userId)).first();
            if (doc == null) {
                return Optional.empty();
            }
            return Optional.of(new PlayerStats(
                    getInt(doc, "id", 0),
                    getInt(doc, "user_id", userId),
                    getInt(doc, "games_played", 0),
                    getInt(doc, "highest_score", 0),
                    getInt(doc, "highest_wave", 0),
                    getInt(doc, "correct_answers", 0),
                    getInt(doc, "incorrect_answers", 0),
                    getInt(doc, "total_enemies_defeated", 0)));
        } catch (Exception e) {
            throw new SQLException("MongoDB error finding player stats: " + e.getMessage(), e);
        }
    }

    /** @return a zeroed stats object when the document does not exist (new account) */
    public PlayerStats getStatsOrEmpty(int userId) throws SQLException {
        return findStats(userId).orElseGet(() -> PlayerStats.empty(userId));
    }

    private static int getInt(Document doc, String field, int fallback) {
        Object val = doc.get(field);
        return val instanceof Number n ? n.intValue() : fallback;
    }
}
