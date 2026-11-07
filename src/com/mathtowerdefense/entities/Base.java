package com.mathtowerdefense.entities;

/**
 * The player's fortress at the end of the enemy path. When its health
 * reaches zero the game is lost.
 */
public class Base {

    private final double x;
    private final double y;
    private final int maxHealth;
    private int health;

    public Base(double x, double y, int maxHealth) {
        this.x = x;
        this.y = y;
        this.maxHealth = Math.max(1, maxHealth);
        this.health = this.maxHealth;
    }

    /**
     * Applies damage when an enemy leaks through.
     *
     * @return remaining health (never negative)
     */
    public int takeDamage(int amount) {
        if (amount > 0) {
            health = Math.max(0, health - amount);
        }
        return health;
    }

    public boolean isDestroyed() { return health <= 0; }

    public int getHealth() { return health; }

    public int getMaxHealth() { return maxHealth; }

    public double getHealthFraction() {
        return maxHealth <= 0 ? 0.0 : (double) health / maxHealth;
    }

    public double getX() { return x; }

    public double getY() { return y; }
}
