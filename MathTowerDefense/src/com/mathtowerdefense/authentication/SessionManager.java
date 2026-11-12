package com.mathtowerdefense.authentication;

import com.mathtowerdefense.models.User;

/**
 * Tracks who is currently signed in (and whether they are a guest).
 * Application-wide singleton - the single source of truth for the session.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private boolean guestMode;

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    /** Successful login: a registered database user. */
    public void login(User user) {
        if (user == null || user.isGuest()) {
            throw new IllegalArgumentException("Session requires a registered user.");
        }
        this.currentUser = user;
        this.guestMode = false;
    }

    /** Guest play: no account, nothing is saved to the database. */
    public void startGuestSession() {
        this.currentUser = User.guest();
        this.guestMode = true;
    }

    /** Logout: clears the session completely. */
    public void logout() {
        this.currentUser = null;
        this.guestMode = false;
    }

    public boolean isLoggedIn() { return currentUser != null && !guestMode; }

    public boolean isGuest() { return guestMode && currentUser != null; }

    public boolean isActive() { return currentUser != null; }

    /** @return the current user or {@code null} when logged out */
    public User getCurrentUser() { return currentUser; }

    /** @return the display name ("Guest" when in guest mode, "—" when logged out) */
    public String getDisplayUsername() {
        if (currentUser == null) {
            return "—";
        }
        return currentUser.getUsername();
    }
}
