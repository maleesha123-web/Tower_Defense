package com.mathtowerdefense.entities;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Base class for all enemies that travel along the predefined path.
 *
 * Uses a waypoint list (composition) supplied by the map: the enemy advances
 * toward the next waypoint each update and reports when it reaches the base.
 * Subclasses (Normal/Fast/Tank/Boss) only differ in the {@link EnemyType}
 * they are constructed with - behaviour differences are data-driven, which
 * keeps one robust movement implementation for all of them.
 */
public class Enemy {

    private final EnemyType type;
    private double x;
    private double y;
    private int health;
    private final int maxHealth;
    private double speed;
    private final int damage;
    private final int reward;
    private boolean alive = true;
    private boolean reachedBase = false;

    /** Shared waypoint list of the current map (never mutated by enemies). */
    private final List<Point2D.Double> waypoints;
    private int waypointIndex = 1;
    private double distanceTravelled = 0.0;

    public Enemy(EnemyType type, double startX, double startY, double healthMultiplier,
                 double speedMultiplier, List<Point2D.Double> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("Enemy requires at least one waypoint.");
        }
        this.type = type;
        this.x = startX;
        this.y = startY;
        this.waypoints = waypoints;
        this.maxHealth = Math.max(1, (int) Math.round(type.getBaseHealth() * healthMultiplier));
        this.health = maxHealth;
        this.speed = Math.max(5.0, type.getBaseSpeed() * speedMultiplier);
        this.damage = type.getBaseDamage();
        this.reward = type.getBaseReward();
    }

    /**
     * Moves the enemy toward its next waypoint.
     *
     * @param dt elapsed time in seconds since the previous frame
     * @return true when the enemy just reached the player base
     */
    public boolean update(double dt) {
        if (!alive || reachedBase) {
            return false;
        }
        Point2D.Double target = waypoints.get(Math.min(waypointIndex, waypoints.size() - 1));
        double dx = target.x - x;
        double dy = target.y - y;
        double distance = Math.hypot(dx, dy);
        double step = speed * dt;

        if (distance <= step) {
            x = target.x;
            y = target.y;
            distanceTravelled += distance;
            if (waypointIndex < waypoints.size() - 1) {
                waypointIndex++;
            } else {
                reachedBase = true; // leaked through to the base
                return true;
            }
        } else if (distance > 0.0001) {
            x += dx / distance * step;
            y += dy / distance * step;
            distanceTravelled += step;
        }
        return false;
    }

    /**
     * Applies damage to this enemy.
     *
     * @return true when this hit killed the enemy (only the first lethal hit)
     */
    public boolean takeDamage(int amount) {
        if (!alive || amount <= 0) {
            return false;
        }
        health -= amount;
        if (health <= 0) {
            health = 0;
            alive = false;
            return true;
        }
        return false;
    }

    /**
     * Removes the enemy from play without a kill (used when it leaked
     * through to the base). In-flight projectiles then fizzle instead of
     * awarding damage/rewards for an enemy that already escaped.
     */
    public void expire() {
        alive = false;
    }

    /** Manhattan-free Euclidean distance to a point (used by tower targeting). */
    public double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public EnemyType getType() { return type; }

    public double getX() { return x; }

    public double getY() { return y; }

    public int getHealth() { return health; }

    public int getMaxHealth() { return maxHealth; }

    /** 0.0 .. 1.0 - used for the health bar above the enemy. */
    public double getHealthFraction() {
        return maxHealth <= 0 ? 0.0 : (double) health / maxHealth;
    }

    public double getSpeed() { return speed; }

    public int getDamage() { return damage; }

    public int getReward() { return reward; }

    public int getRadius() { return type.getRadius(); }

    public boolean isAlive() { return alive; }

    /** True when the enemy leaked through and must damage the base. */
    public boolean hasReachedBase() { return reachedBase; }

    /** How far the enemy travelled along the path - "most advanced" targeting. */
    public double getDistanceTravelled() { return distanceTravelled; }

    public boolean isBoss() { return type.isBoss(); }
}
