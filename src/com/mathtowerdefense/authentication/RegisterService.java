package com.mathtowerdefense.authentication;

import com.mathtowerdefense.database.DatabaseConnection;
import com.mathtowerdefense.database.UserDAO;
import com.mathtowerdefense.models.User;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Registration use-case: validates the form, rejects duplicates, hashes the
 * password and inserts the new account. No SQL appears outside the DAO layer.
 */
public final class RegisterService {

    /** Pragmatic email check - deliberately simple and readable. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 20;
    public static final int MIN_PASSWORD = 6;

    private final UserDAO userDAO;
    private final PasswordHasher hasher;

    public RegisterService() {
        this(new UserDAO(), new PasswordHasher());
    }

    public RegisterService(UserDAO userDAO, PasswordHasher hasher) {
        this.userDAO = userDAO;
        this.hasher = hasher;
    }

    /**
     * Pure validation of the registration form (no database access) so it can
     * be unit tested and reused by the GUI for live feedback.
     *
     * @return the first error message found, or {@code null} when everything is valid
     */
    public static String validate(String username, String email, String password, String confirm) {
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirm)) {
            return "All fields are required.";
        }
        username = username.trim();
        email = email.trim();

        if (username.length() < MIN_USERNAME || username.length() > MAX_USERNAME) {
            return "Username must be " + MIN_USERNAME + "-" + MAX_USERNAME + " characters long.";
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            return "Username may only contain letters, digits and underscores.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        if (password.length() < MIN_PASSWORD) {
            return "Password must be at least " + MIN_PASSWORD + " characters long.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        return null;
    }

    /**
     * Creates the account when validation and duplicate checks pass.
     *
     * @return success/failure with a friendly message for the GUI
     */
    public Result register(String username, String email, String password, String confirm) {
        String inputError = validate(username, email, password, confirm);
        if (inputError != null) {
            return Result.failure(inputError);
        }
        username = username.trim();
        email = email.trim();
        try {
            if (userDAO.findByUsername(username).isPresent()) {
                return Result.failure("That username is already taken.");
            }
            if (userDAO.findByEmail(email).isPresent()) {
                return Result.failure("An account with that email already exists.");
            }
            String hash = hasher.hash(password); // never store plain text
            User user = new User(0, username, email, hash, null);
            if (!userDAO.insert(user)) {
                return Result.failure("Could not create the account. Please try again.");
            }
            return Result.success("Account created. You can now log in.", user);
        } catch (SQLException dbProblem) {
            return Result.failure(DatabaseConnection.unavailableMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Same result type as {@link LoginService.Result} (kept package-local). */
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
