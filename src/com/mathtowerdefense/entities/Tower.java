package com.mathtowerdefense.entities;

import java.awt.Color;
import java.util.List;

/**
 * A defensive tower placed on the map.
 *
 * Subclasses (Basic/Rapid/Power/MathBoost) only differ in the
 * {@link TowerType} they bind and in optional behavioural hooks such as
 * {@link #onCorrectAnswer(Player)} - keeping targeting, cooldown, upgrades
 * and projectiles in ONE tested implementation (inheritance only where it
 * genuinely adds behaviour, per the design guidelines).
 */
public class Tower {

    private final TowerType type;
    private final double x;
    private final double y;
    private int level = 1;
    private double cooldown;      // seconds until the next shot
    private Enemy target;         // current target (null = none)

    public Tower(TowerType type, double x, double y) {
        if (type == null) {
            throw new IllegalArgumentException("Tower requires a type.");
        }
        this.type = type;
        this.x = x;
        this.y = y;
    }

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    /** Reduces the cooldown; called once per frame. */
    public void update(double dt) {
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - dt);
        }
    }

    public boolean canFire() { return cooldown <= 0; }

    /**
     * Fires a shot at the current target and restarts the attack cooldown.
     *
     * @param damageMultiplier global multiplier (math power / weakened state / level bonus)
     * @return the projectile, or {@code null} when on cooldown or without target
     */
    public Projectile fire(double damageMultiplier) {
        if (!canFire() || target == null || !target.isAlive()) {
            return null;
        }
        cooldown = type.getAttackInterval();
        int damage = Math.max(1, (int) Math.round(getDamage() * damageMultiplier));
        Projectile p = new Projectile(x, y, target, damage);
        return p;
    }

    /**
     * Picks the enemy that has travelled furthest along the path while inside
     * range (classic "first" targeting - protects the base best).
     *
     * @return the chosen enemy or {@code null} when nothing is in range
     */
    public Enemy findTarget(List<Enemy> enemies) {
        Enemy best = null;
        double bestProgress = -1;
        double range = getRange();
        for (Enemy e : enemies) {
            if (!e.isAlive() || e.hasReachedBase()) {
                continue;
            }
            if (e.distanceTo(x, y) <= range && e.getDistanceTravelled() > bestProgress) {
                best = e;
                bestProgress = e.getDistanceTravelled();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Stats & upgrades
    // ------------------------------------------------------------------

    /** Damage at the current upgrade level (10 / 20 / 35 / 55 for BASIC). */
    public int getDamage() {
        return Math.max(1, (int) Math.round(
                type.getBaseDamage() * TowerType.damageMultiplierForLevel(level)));
    }

    public double getRange() { return type.getRange(); }

    public int getCost() { return type.getCost(); }

    /** Cost of the next upgrade, or -1 at max level. */
    public int getUpgradeCost() {
        return TowerType.upgradeCostForLevel(level, type.getCost());
    }

    public boolean isMaxLevel() { return level >= TowerType.MAX_LEVEL; }

    /**
     * Upgrades the tower one level (caller validates + spends the coins).
     *
     * @return true when the level actually increased
     */
    public boolean upgrade() {
        if (isMaxLevel()) {
            return false;
        }
        level++;
        return true;
    }

    // ------------------------------------------------------------------
    // Polymorphic hooks
    // ------------------------------------------------------------------

    /**
     * Called by the engine after every CORRECT math answer so special towers
     * can reward the player. Base implementation does nothing; see
     * {@link MathBoostTower} for the override.
     *
     * @return a short message describing the bonus, or {@code null} for no bonus
     */
    public String onCorrectAnswer(Player player) {
        return null;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public TowerType getType() { return type; }

    public String getDisplayName() { return type.getDisplayName(); }

    public String getDescription() { return type.getDescription(); }

    public Color getColor() { return type.getColor(); }

    public double getX() { return x; }

    public double getY() { return y; }

    public int getLevel() { return level; }

    public Enemy getTarget() { return target; }

    public void setTarget(Enemy target) { this.target = target; }

    public double getCooldownFraction() {
        double interval = type.getAttackInterval();
        return interval <= 0 ? 0 : cooldown / interval;
    }
}
