package com.mathtowerdefense.managers;

import com.mathtowerdefense.database.DatabaseConnection;
import com.mathtowerdefense.database.LeaderboardDAO;
import com.mathtowerdefense.database.ScoreDAO;
import com.mathtowerdefense.entities.Player;
import com.mathtowerdefense.models.PlayerStats;
import com.mathtowerdefense.models.Score;
import com.mathtowerdefense.models.User;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service layer between the GUI and the DAO layer (spec section 48:
 * GUI -> Service -> DAO -> MySQL). Contains NO SQL itself, converts
 * SQLExceptions into friendly messages and keeps the GUI database-free.
 */
public class ScoreService {

    private final ScoreDAO scoreDAO;
    private final LeaderboardDAO leaderboardDAO;
    private String lastError;

    public ScoreService() {
        this(new ScoreDAO(), new LeaderboardDAO());
    }

    public ScoreService(ScoreDAO scoreDAO, LeaderboardDAO leaderboardDAO) {
        this.scoreDAO = scoreDAO;
        this.leaderboardDAO = leaderboardDAO;
    }

    /**
     * Persists a finished game (score row + lifetime stats).
     *
     * @return true when saved; false with {@link #getLastError()} describing
     *         why (database unavailable, guest session, ...)
     */
    public boolean saveGameResult(User user, Player player, int waveReached) {
        lastError = null;
        if (user == null || user.isGuest()) {
            lastError = "Guest session - score not saved.";
            return false;
        }
        try {
            scoreDAO.saveGameResult(
                    user.getId(),
                    player.getScore(),
                    player.getLevel(),
                    waveReached,
                    player.getCorrectAnswers(),
                    player.getIncorrectAnswers(),
                    player.getEnemiesDefeated());
            return true;
        } catch (SQLException e) {
            lastError = DatabaseConnection.unavailableMessage();
            return false;
        }
    }

    /**
     * Loads the top scores for the leaderboard screen.
     *
     * @return the rows (possibly empty); on failure an empty list with
     *         {@link #getLastError()} set to a friendly message
     */
    public List<Score> loadLeaderboard(int limit) {
        lastError = null;
        try {
            return leaderboardDAO.getTopScores(limit);
        } catch (SQLException e) {
            lastError = DatabaseConnection.unavailableMessage();
            return Collections.emptyList();
        }
    }

    /**
     * Loads lifetime statistics for the profile screen.
     *
     * @return zeroed stats when unavailable; {@link #getLastError()} explains why
     */
    public PlayerStats loadProfile(User user) {
        lastError = null;
        if (user == null || user.isGuest()) {
            lastError = "Guest session - create an account to track your statistics.";
            return PlayerStats.empty(0);
        }
        try {
            return scoreDAO.getStatsOrEmpty(user.getId());
        } catch (SQLException e) {
            lastError = DatabaseConnection.unavailableMessage();
            return PlayerStats.empty(user.getId());
        }
    }

    /** Quick database availability probe for the UI. */
    public boolean isDatabaseAvailable() {
        return DatabaseConnection.isAvailable();
    }

    /** Friendly message for the most recent failure (null when none). */
    public String getLastError() {
        return lastError;
    }
}
