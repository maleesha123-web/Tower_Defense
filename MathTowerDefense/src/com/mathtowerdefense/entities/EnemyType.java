package com.mathtowerdefense.entities;

import java.awt.Color;

/**
 * Statistics and appearance of each enemy kind.
 * Values are the wave-1 base figures; WaveManager scales them per wave and
 * difficulty (see {@code healthMultiplier} / {@code speedMultiplier}).
 */
public enum EnemyType {

    NORMAL("Normal", 60, 55.0, 5, 25, 14, new Color(231, 76, 60), "N"),
    FAST("Fast", 35, 95.0, 3, 35, 11, new Color(241, 196, 15), "F"),
    TANK("Tank", 170, 30.0, 12, 60, 19, new Color(127, 140, 141), "T"),
    BOSS("Boss", 700, 34.0, 25, 250, 27, new Color(155, 89, 182), "B");

    private final String displayName;
    private final int baseHealth;
    private final double baseSpeed;   // pixels per second
    private final int baseDamage;     // damage to the base when the enemy leaks
    private final int baseReward;     // coins awarded when killed
    private final int radius;
    private final Color color;
    private final String badge;

    EnemyType(String displayName, int baseHealth, double baseSpeed, int baseDamage,
              int baseReward, int radius, Color color, String badge) {
        this.displayName = displayName;
        this.baseHealth = baseHealth;
        this.baseSpeed = baseSpeed;
        this.baseDamage = baseDamage;
        this.baseReward = baseReward;
        this.radius = radius;
        this.color = color;
        this.badge = badge;
    }

    public String getDisplayName() { return displayName; }

    public int getBaseHealth() { return baseHealth; }

    public double getBaseSpeed() { return baseSpeed; }

    public int getBaseDamage() { return baseDamage; }

    public int getBaseReward() { return baseReward; }

    public int getRadius() { return radius; }

    public Color getColor() { return color; }

    public String getBadge() { return badge; }

    public boolean isBoss() { return this == BOSS; }
}
