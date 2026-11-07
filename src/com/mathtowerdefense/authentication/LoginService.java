package com.mathtowerdefense.authentication;

import com.mathtowerdefense.database.DatabaseConnection;
import com.mathtowerdefense.database.UserDAO;
import com.mathtowerdefense.models.User;

import java.sql.SQLException;

/**
 * Login use-case: validates input, looks the account up in the database and
 * verifies the password hash. On success the {@link SessionManager} is updated.
 *
 * Validation logic is static and GUI-independent so it can be unit tested.
 */
public final class LoginService {

    private final UserDAO userDAO;
    private final PasswordHasher hasher;

    public LoginService() {
        this(new UserDAO(), new PasswordHasher());
    }

    public LoginService(UserDAO userDAO, PasswordHasher hasher) {
        this.userDAO = userDAO;
        this.hasher = hasher;
    }

    /**
     * Input validation without any database access.
     *
     * @return an error message, or {@code null} when the input is valid
     */
    public static String validate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return "Username must not be empty.";
        }
        if (password == null || password.isEmpty()) {
            return "Password must not be empty.";
        }
        if (username.trim().length() > 20) {
            return "Username must be 20 characters or fewer.";
        }
        return null;
    }

    /**
     * Attempts to sign a user in.
     *
     * @return a successful result with the user, or a failure with a friendly,
     *         stack-trace-free message (invalid credentials, database offline, ...)
     */
    public Result login(String username, String password) {
        String inputError = validate(username, password);
        if (inputError != null) {
            return Result.failure(inputError);
        }
        try {
            User user = userDAO.findByUsername(username.trim()).orElse(null);
            // Same message for "no such user" and "wrong password" so the
            // endpoint cannot be used to enumerate accounts.
            if (user == null || !hasher.verify(password, user.getPasswordHash())) {
                return Result.failure("Invalid username or password.");
            }
            SessionManager.getInstance().login(user);
            return Result.success("Welcome back, " + user.getUsername() + "!", user);
        } catch (SQLException dbProblem) {
            return Result.failure(DatabaseConnection.unavailableMessage());
        }
    }

    /** Result object returned to the GUI layer. */
    public static final class Result {
        private final boolean success;
        private final String message;
        private final User user;

        private Result(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        static Result success(String message, User user) { return new Result(true, message, user); }

        static Result failure(String message) { return new Result(false, message, null); }

        public boolean isSuccess() { return success; }

        public String getMessage() { return message; }

        public User getUser() { return user; }
    }
}
