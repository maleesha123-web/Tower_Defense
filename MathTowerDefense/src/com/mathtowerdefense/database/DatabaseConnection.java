package com.mathtowerdefense.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single place where the MongoDB connection is created and managed.
 *
 * Configured for MongoDB Compass / local instance at:
 *   mongodb://localhost:27017
 *
 * Configuration source order:
 *   1. environment variables MTD_DB_URL / MTD_DB_NAME (see .env.example)
 *   2. Defaults: mongodb://localhost:27017, database: mathtowerdefense
 *
 * Automatically manages indexes and auto-seeds initial leaderboard records if empty.
 */
public final class DatabaseConnection {

    public static final String DEFAULT_URL = "mongodb://localhost:27017";
    public static final String DEFAULT_DATABASE = "mathtowerdefense";

    private static MongoClient mongoClient;
    private static volatile boolean initialized = false;
    private static volatile boolean serverStartAttempted = false;
    private static final Object LOCK = new Object();

    static {
        // Suppress SLF4J missing warnings and MongoDB internal connection logs.
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        Logger.getLogger("com.mongodb").setLevel(Level.SEVERE);
    }

    private DatabaseConnection() {}

    /** @return MongoDB connection URI (from MTD_DB_URL or default). */
    public static String getUrl() {
        String env = System.getenv("MTD_DB_URL");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_URL;
    }

    /** @return target database name (from MTD_DB_NAME or default). */
    public static String getDatabaseName() {
        String env = System.getenv("MTD_DB_NAME");
        return (env != null && !env.isBlank()) ? env.trim() : DEFAULT_DATABASE;
    }

    /**
     * Obtains the shared MongoClient instance with responsive timeout settings.
     */
    public static MongoClient getClient() {
        synchronized (LOCK) {
            if (mongoClient == null) {
                ensureLocalServerRunning();
                String uri = getUrl();
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .applyToSocketSettings(builder ->
                                builder.connectTimeout(3000, TimeUnit.MILLISECONDS)
                                       .readTimeout(5000, TimeUnit.MILLISECONDS))
                        .applyToClusterSettings(builder ->
                                builder.serverSelectionTimeout(2500, TimeUnit.MILLISECONDS))
                        .build();
                mongoClient = MongoClients.create(settings);
            }
            return mongoClient;
        }
    }

    /**
     * If configured for localhost:27017 and port 27017 is not yet open,
     * attempts to spin up mongod.exe automatically in the background.
     */
    private static void ensureLocalServerRunning() {
        if (serverStartAttempted) {
            return;
        }
        serverStartAttempted = true;
        String uri = getUrl();
        if (!uri.contains("localhost:27017") && !uri.contains("127.0.0.1:27017")) {
            return;
        }
        if (isPortOpen("127.0.0.1", 27017)) {
            return;
        }

        File mongod = new File("C:\\Program Files\\MongoDB\\Server\\8.3\\bin\\mongod.exe");
        if (!mongod.exists()) {
            return;
        }

        File dbPath = new File("C:\\Users\\HP\\Desktop\\Fish\\mongodb_data");
        if (!dbPath.exists()) {
            dbPath.mkdirs();
        }

        try {
            new ProcessBuilder(
                    mongod.getAbsolutePath(),
                    "--dbpath", dbPath.getAbsolutePath(),
                    "--port", "27017",
                    "--bind_ip", "127.0.0.1"
            ).redirectErrorStream(true).start();

            // Short pause to allow mongod to bind the socket
            for (int i = 0; i < 15; i++) {
                if (isPortOpen("127.0.0.1", 27017)) {
                    break;
                }
                Thread.sleep(200);
            }
        } catch (Exception ignored) {}
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 400);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return the active MongoDatabase instance, ensuring indexes & seeds are checked.
     */
    public static MongoDatabase getDatabase() {
        MongoDatabase db = getClient().getDatabase(getDatabaseName());
        ensureInitialized(db);
        return db;
    }

    /**
     * Closes the active client connection.
     */
    public static void close() {
        synchronized (LOCK) {
            if (mongoClient != null) {
                try {
                    mongoClient.close();
                } catch (Exception ignored) {}
                mongoClient = null;
                initialized = false;
            }
        }
    }

    /**
     * Atomic, thread-safe sequence generator for integer IDs (users, scores, stats).
     */
    public static int getNextSequence(String sequenceName) {
        MongoCollection<Document> counters = getClient()
                .getDatabase(getDatabaseName())
                .getCollection("counters");

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        Document doc = counters.findOneAndUpdate(
                Filters.eq("_id", sequenceName),
                Updates.inc("seq", 1),
                options);

        if (doc != null && doc.get("seq") instanceof Number n) {
            return n.intValue();
        }
        return 1;
    }

    /**
     * Checks if MongoDB is reachable at the configured connection URL.
     */
    public static boolean isAvailable() {
        try {
            Document ping = new Document("ping", 1);
            getClient().getDatabase(getDatabaseName()).runCommand(ping);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Friendly explanation for the UI when MongoDB is unavailable. */
    public static String unavailableMessage() {
        return "Database unavailable. Please ensure MongoDB is running at " + getUrl();
    }

    /**
     * Ensures collection indexes exist and seeds sample records if database is empty.
     */
    private static void ensureInitialized(MongoDatabase db) {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            try {
                MongoCollection<Document> users = db.getCollection("users");
                users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
                users.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));

                MongoCollection<Document> scores = db.getCollection("scores");
                scores.createIndex(Indexes.descending("score"));

                MongoCollection<Document> stats = db.getCollection("player_stats");
                stats.createIndex(Indexes.ascending("user_id"), new IndexOptions().unique(true));

                // Seed sample data if newly installed / empty
                if (users.countDocuments() == 0) {
                    seedSampleData(db, users, scores, stats);
                }
                initialized = true;
            } catch (Exception e) {
                // If offline or initial probe, do not fail startup
            }
        }
    }

    private static void seedSampleData(MongoDatabase db,
                                       MongoCollection<Document> users,
                                       MongoCollection<Document> scores,
                                       MongoCollection<Document> stats) {
        MongoCollection<Document> counters = db.getCollection("counters");

        String placeholderHash = "pbkdf2$120000$sampleSeedOnlyNotARealHash$0000000000000000000000000000000000000000000000000000000000000000";
        Date now = new Date();

        // 1. MathMaster
        users.insertOne(new Document("id", 1)
                .append("username", "MathMaster")
                .append("email", "mathmaster@example.com")
                .append("password_hash", placeholderHash)
                .append("created_at", now));

        scores.insertOne(new Document("id", 1)
                .append("user_id", 1)
                .append("username", "MathMaster")
                .append("score", 12450)
                .append("level", 8)
                .append("wave", 9)
                .append("enemies_defeated", 96)
                .append("created_at", now));

        stats.insertOne(new Document("id", 1)
                .append("user_id", 1)
                .append("games_played", 12)
                .append("highest_score", 12450)
                .append("highest_wave", 9)
                .append("correct_answers", 140)
                .append("incorrect_answers", 22)
                .append("total_enemies_defeated", 310));

        // 2. TowerKing
        users.insertOne(new Document("id", 2)
                .append("username", "TowerKing")
                .append("email", "towerking@example.com")
                .append("password_hash", placeholderHash)
                .append("created_at", now));

        scores.insertOne(new Document("id", 2)
                .append("user_id", 2)
                .append("username", "TowerKing")
                .append("score", 10800)
                .append("level", 7)
                .append("wave", 8)
                .append("enemies_defeated", 81)
                .append("created_at", now));

        stats.insertOne(new Document("id", 2)
                .append("user_id", 2)
                .append("games_played", 10)
                .append("highest_score", 10800)
                .append("highest_wave", 8)
                .append("correct_answers", 118)
                .append("incorrect_answers", 30)
                .append("total_enemies_defeated", 264));

        // 3. PlayerFour
        users.insertOne(new Document("id", 3)
                .append("username", "PlayerFour")
                .append("email", "playerfour@example.com")
                .append("password_hash", placeholderHash)
                .append("created_at", now));

        scores.insertOne(new Document("id", 3)
                .append("user_id", 3)
                .append("username", "PlayerFour")
                .append("score", 9750)
                .append("level", 6)
                .append("wave", 7)
                .append("enemies_defeated", 74)
                .append("created_at", now));

        stats.insertOne(new Document("id", 3)
                .append("user_id", 3)
                .append("games_played", 8)
                .append("highest_score", 9750)
                .append("highest_wave", 7)
                .append("correct_answers", 95)
                .append("incorrect_answers", 25)
                .append("total_enemies_defeated", 219));

        counters.replaceOne(Filters.eq("_id", "users"), new Document("_id", "users").append("seq", 3), new com.mongodb.client.model.ReplaceOptions().upsert(true));
        counters.replaceOne(Filters.eq("_id", "scores"), new Document("_id", "scores").append("seq", 3), new com.mongodb.client.model.ReplaceOptions().upsert(true));
        counters.replaceOne(Filters.eq("_id", "player_stats"), new Document("_id", "player_stats").append("seq", 3), new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    public static LocalDateTime toLocalDateTime(Date d) {
        if (d == null) return LocalDateTime.now();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date toDate(LocalDateTime ldt) {
        if (ldt == null) return new Date();
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
