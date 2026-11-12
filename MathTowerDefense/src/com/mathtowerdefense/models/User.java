package com.mathtowerdefense.models;

import java.time.LocalDateTime;

/**
 * A registered user account (persistent, loaded from / stored in MySQL).
 * For guests a synthetic instance with {@code id = -1} is used so that the
 * rest of the application never has to handle a null session user.
 */
public final class User {

    public static final int GUEST_ID = -1;

    private int id;
    private final String username;
    private final String email;
    private final String passwordHash; // null for guests
    private final LocalDateTime createdAt;

    public User(int id, String username, String email, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /** Creates a guest session user (no database identity). */
    public static User guest() {
        return new User(GUEST_ID, "Guest", null, null, LocalDateTime.now());
    }

    public boolean isGuest() { return id == GUEST_ID; }

    public int getId() { return id; }

    /** Used by DAOs (different package) after inserting a row with a generated key. */
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }

    public String getEmail() { return email; }

    public String getPasswordHash() { return passwordHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "User{" + username + (isGuest() ? " (guest)" : ", id=" + id) + "}";
    }
}
