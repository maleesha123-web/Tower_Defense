package com.mathtowerdefense.entities;

import java.awt.Color;

/**
 * Static catalogue of the four tower types: cost, base damage, range and
 * attack interval. Behavioural/visual differences live in the Tower
 * subclasses; this enum is the shared data table.
 */
public enum TowerType {

    BASIC("Basic Tower", 100, 10, 115, 1.0,
            "Cheap all-rounder. Reliable damage at a fair price.", new Color(46, 134, 255)),

    RAPID("Rapid Tower", 160, 4, 95, 0.3,
            "Low damage but a very high rate of fire.", new Color(46, 204, 113)),

    POWER("Power Tower", 240, 30, 155, 1.6,
            "Expensive, slow, and devastatingly strong.", new Color(230, 126, 34)),

    MATH_BOOST("Math Boost Tower", 140, 7, 120, 1.0,
            "Extra rewards for every correct math answer.", new Color(155, 89, 182));

    private final String displayName;
    private final int cost;
    private final int baseDamage;
    private final double range;
    private final double attackInterval; // seconds between shots
    private final String description;
    private final Color color;

    TowerType(String displayName, int cost, int baseDamage, double range,
              double attackInterval, String description, Color color) {
        this.displayName = displayName;
        this.cost = cost;
        this.baseDamage = baseDamage;
        this.range = range;
        this.attackInterval = attackInterval;
        this.description = description;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }

    public int getCost() { return cost; }

    public int getBaseDamage() { return baseDamage; }

    public double getRange() { return range; }

    public double getAttackInterval() { return attackInterval; }

    public String getDescription() { return description; }

    public Color getColor() { return color; }

    /** Max tower level (spec: levels 1-4). */
    public static final int MAX_LEVEL = 4;

    /** Damage multiplier per level: 10 / 20 / 35 / 55 for a 10-damage tower. */
    public static double damageMultiplierForLevel(int level) {
        return switch (Math.max(1, Math.min(level, MAX_LEVEL))) {
            case 2 -> 2.0;
            case 3 -> 3.5;
            case 4 -> 5.5;
            default -> 1.0;
        };
    }

    /**
     * Upgrade cost from {@code level} to {@code level + 1} - increases with
     * every level as required by the specification.
     *
     * @return the cost, or -1 when the tower is already max level
     */
    public static int upgradeCostForLevel(int level, int baseCost) {
        if (level >= MAX_LEVEL) {
            return -1;
        }
        return switch (level) {
            case 1 -> (int) Math.round(baseCost * 0.6);
            case 2 -> (int) Math.round(baseCost * 1.0);
            default -> (int) Math.round(baseCost * 1.4);
        };
    }
}
