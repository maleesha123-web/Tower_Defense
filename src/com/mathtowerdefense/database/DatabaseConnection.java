package com.mathtowerdefense.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single place where the MySQL (XAMPP) connection is created.
 *
 * Configuration source order:
 *   1. environment variables MTD_DB_URL / MTD_DB_USER / MTD_DB_PASSWORD
 *      (see .env.example - never hard-code secrets in source)
 *   2. XAMPP defaults: jdbc:mysql://localhost:3306/mathtowerdefense, root, ""
 *
 * There are NO credentials inside the source code. Every method fails with a
 * plain SQLException which the service layer converts into a friendly message -
 * the game stays fully playable (guest mode) when the database is offline.
 */
public final class DatabaseConnection {

    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/mathtowerdefense"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            + "&connectTimeout=3000&socketTimeout=8000";
    public static final String DEFAULT_USER = "root";

    private DatabaseConnection() {}

    private static String url() {
        return envOrDefault("MTD_DB_URL", DEFAULT_URL);
    }

    private static String user() {
        return envOrDefault("MTD_DB_USER", DEFAULT_USER);
    }

    private static String password() {
        return envOrDefault("MTD_DB_PASSWORD", "");
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /**
     * Opens a new connection using the MySQL JDBC driver.
     *
     * @throws SQLException when the driver is missing or MySQL is not reachable
     */
    public static Connection getConnection() throws SQLException {
        // Load the driver explicitly when it is on the classpath; with JDBC 4+
        // the ServiceLoader usually does this automatically. If the JAR is
        // missing entirely, DriverManager throws "No suitable driver", which
        // callers translate into a friendly "database unavailable" message.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException driverNotInstalled) {
            // Fall through - DriverManager will raise a clear SQLException.
        }
        return DriverManager.getConnection(url(), user(), password());
    }

    /**
     * Quick availability probe with a short validation timeout.
     *
     * @return true when a connection can be opened and validated
     */
    public static boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Friendly, stack-trace-free explanation for the UI. */
    public static String unavailableMessage() {
        return "Database unavailable. Start MySQL (XAMPP) and import mathtowerdefense.sql.";
    }
}
