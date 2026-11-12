package com.mathtowerdefense.entities;

import java.awt.Color;

/**
 * A visible shot travelling from a tower to its target enemy.
 * On impact the projectile damages the enemy exactly once, then deactivates
 * and is removed by the {@code CollisionManager}.
 */
public class Projectile {

    private static final double SPEED = 420.0; // pixels per second
    private static final double IMPACT_DISTANCE = 8.0;

    private double x;
    private double y;
    private final Enemy target;
    private final int damage;
    private boolean active = true;
    private boolean hit = false;

    public Projectile(double startX, double startY, Enemy target, int damage) {
        this.x = startX;
        this.y = startY;
        this.target = target;
        this.damage = Math.max(1, damage);
    }

    /**
     * Moves the projectile toward the target and applies damage on impact.
     *
     * @param dt elapsed seconds since the previous frame
     * @return true when this update landed a hit
     */
    public boolean update(double dt) {
        if (!active) {
            return false;
        }
        // Target died or vanished - fizzle the shot (standard tower-defense behaviour).
        if (target == null || !target.isAlive()) {
            active = false;
            return false;
        }

        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double distance = Math.hypot(dx, dy);

        if (distance <= IMPACT_DISTANCE) {
            target.takeDamage(damage);
            active = false;
            hit = true;
            return true;
        }

        double step = SPEED * dt;
        if (step >= distance) {
            x = target.getX();
            y = target.getY();
        } else {
            x += dx / distance * step;
            y += dy / distance * step;
        }
        return false;
    }

    public boolean isActive() { return active; }

    public boolean hasHit() { return hit; }

    public Enemy getTarget() { return target; }

    public int getDamage() { return damage; }

    public double getX() { return x; }

    public double getY() { return y; }

    public Color getColor() { return new Color(255, 236, 140); }
}
