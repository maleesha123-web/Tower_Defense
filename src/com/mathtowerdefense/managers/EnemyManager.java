package com.mathtowerdefense.managers;

import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.EnemyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns every enemy currently on the map: spawning, per-frame movement and
 * removal of dead/leaked enemies. Keeps enemy lifecycle out of GameEngine.
 */
public class EnemyManager {

    private final List<Enemy> enemies = new ArrayList<>();

    /**
     * Adds a new enemy at the start of the path.
     *
     * @param waypoints      shared path of the current map
     * @param healthMultiplier wave/difficulty health scaling
     * @param speedMultiplier  wave/difficulty speed scaling
     * @return the created enemy
     */
    public Enemy spawn(EnemyType type, List<java.awt.geom.Point2D.Double> waypoints,
                       double healthMultiplier, double speedMultiplier) {
        java.awt.geom.Point2D.Double start = waypoints.get(0);
        Enemy enemy = new Enemy(type, start.x, start.y, healthMultiplier, speedMultiplier, waypoints);
        enemies.add(enemy);
        return enemy;
    }

    /**
     * Updates all enemies.
     *
     * @param dt      elapsed seconds
     * @param escaped collector for enemies that just reached the base (damages the base)
     * @return true when at least one enemy leaked through
     */
    public boolean update(double dt, List<Enemy> escaped) {
        boolean anyEscaped = false;
        for (Enemy e : enemies) {
            if (e.update(dt)) {
                anyEscaped = true;
                if (escaped != null) {
                    escaped.add(e);
                }
            }
        }
        return anyEscaped;
    }

    /** Removes dead and leaked enemies from the board. */
    public void removeInactive() {
        enemies.removeIf(e -> !e.isAlive() || e.hasReachedBase());
    }

    public int countAlive() {
        int n = 0;
        for (Enemy e : enemies) {
            if (e.isAlive() && !e.hasReachedBase()) {
                n++;
            }
        }
        return n;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public void clear() {
        enemies.clear();
    }

    /** True when no living enemy remains (used for wave completion). */
    public boolean isEmpty() {
        return countAlive() == 0;
    }
}
