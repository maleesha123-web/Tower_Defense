package com.mathtowerdefense.database;

import com.mathtowerdefense.models.User;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Data access for the {@code users} collection in MongoDB.
 */
public class UserDAO {

    private MongoCollection<Document> collection() {
        return DatabaseConnection.getDatabase().getCollection("users");
    }

    /**
     * Looks up a user by username OR email address (case-insensitive for convenience).
     *
     * @param identifier username or email address
     * @return the matching user, or empty
     */
    public Optional<User> findByUsernameOrEmail(String identifier) throws SQLException {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        try {
            String trimmed = identifier.trim();
            // 1. Fast exact match on username or email
            Document doc = collection().find(Filters.or(
                    Filters.eq("username", trimmed),
                    Filters.eq("email", trimmed)
            )).first();

            // 2. Case-insensitive fallback
            if (doc == null) {
                String escaped = java.util.regex.Pattern.quote(trimmed);
                doc = collection().find(Filters.or(
                        Filters.regex("username", "^" + escaped + "$", "i"),
                        Filters.regex("email", "^" + escaped + "$", "i")
                )).first();
            }

            return doc != null ? Optional.of(map(doc)) : Optional.empty();
        } catch (Exception e) {
            throw new SQLException("MongoDB error finding user by identifier: " + e.getMessage(), e);
        }
    }

    /** @return the user with this username, or empty when nobody is registered */
    public Optional<User> findByUsername(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            String trimmed = username.trim();
            Document doc = collection().find(Filters.eq("username", trimmed)).first();
            if (doc == null) {
                String escaped = java.util.regex.Pattern.quote(trimmed);
                doc = collection().find(Filters.regex("username", "^" + escaped + "$", "i")).first();
            }
            return doc != null ? Optional.of(map(doc)) : Optional.empty();
        } catch (Exception e) {
            throw new SQLException("MongoDB error finding user by username: " + e.getMessage(), e);
        }
    }

    /** @return the user with this email address, or empty */
    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            String trimmed = email.trim();
            Document doc = collection().find(Filters.eq("email", trimmed)).first();
            if (doc == null) {
                String escaped = java.util.regex.Pattern.quote(trimmed);
                doc = collection().find(Filters.regex("email", "^" + escaped + "$", "i")).first();
            }
            return doc != null ? Optional.of(map(doc)) : Optional.empty();
        } catch (Exception e) {
            throw new SQLException("MongoDB error finding user by email: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a new account and assigns the generated key to the given user.
     *
     * @return true when the document was written
     */
    public boolean insert(User user) throws SQLException {
        try {
            int nextId = DatabaseConnection.getNextSequence("users");
            user.setId(nextId);

            Document doc = new Document("id", nextId)
                    .append("username", user.getUsername())
                    .append("email", user.getEmail())
                    .append("password_hash", user.getPasswordHash())
                    .append("created_at", new Date());

            collection().insertOne(doc);
            return true;
        } catch (MongoWriteException mwe) {
            if (mwe.getError().getCode() == 11000) {
                return false; // Duplicate key
            }
            throw new SQLException("MongoDB insert failed: " + mwe.getMessage(), mwe);
        } catch (Exception e) {
            throw new SQLException("MongoDB error inserting user: " + e.getMessage(), e);
        }
    }

    private static User map(Document doc) {
        int id = doc.get("id") instanceof Number n ? n.intValue() : 0;
        String username = doc.getString("username");
        String email = doc.getString("email");
        String passwordHash = doc.getString("password_hash");
        Date date = doc.getDate("created_at");
        return new User(id, username, email, passwordHash, DatabaseConnection.toLocalDateTime(date));
    }
}
