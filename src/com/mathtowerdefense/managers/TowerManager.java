package com.mathtowerdefense.managers;

import com.mathtowerdefense.entities.BasicTower;
import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.MathBoostTower;
import com.mathtowerdefense.entities.PowerTower;
import com.mathtowerdefense.entities.Projectile;
import com.mathtowerdefense.entities.RapidTower;
import com.mathtowerdefense.entities.Tower;
import com.mathtowerdefense.entities.TowerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the towers placed on the map: creation (factory), selection, upgrades
 * and per-frame combat updates (targeting + firing).
 */
public class TowerManager {

    private final List<Tower> towers = new ArrayList<>();
    private Tower selected; // tower currently shown in the info panel

    /**
     * Factory: creates the right subclass for the chosen type at (x, y).
     *
     * @return the new tower
     */
    public Tower createTower(TowerType type, double x, double y) {
        Tower tower = switch (type) {
            case BASIC -> new BasicTower(x, y);
            case RAPID -> new RapidTower(x, y);
            case POWER -> new PowerTower(x, y);
            case MATH_BOOST -> new MathBoostTower(x, y);
        };
        towers.add(tower);
        return tower;
    }

    /**
     * Selects the tower nearest to a map click.
     *
     * @param clickX click position in map coordinates
     * @param pickupRadius how close the click may be
     * @return the selected tower or null when the click hit nothing
     */
    public Tower selectAt(double clickX, double clickY, double pickupRadius) {
        Tower best = null;
        double bestDist = pickupRadius;
        for (Tower t : towers) {
            double d = Math.hypot(t.getX() - clickX, t.getY() - clickY);
            if (d <= bestDist) {
                best = t;
                bestDist = d;
            }
        }
        selected = best;
        return selected;
    }

    /** @return true when a tower exists within {@code radius} of (x, y) (placement check) */
    public boolean isOccupied(double x, double y, double radius) {
        for (Tower t : towers) {
            if (Math.hypot(t.getX() - x, t.getY() - y) < radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates cooldowns, re-acquires targets and fires ready towers.
     *
     * @param dt               elapsed seconds
     * @param enemies          living enemies
     * @param damageMultiplier global damage multiplier (math state)
     * @param outProjectiles   collector for newly fired projectiles
     * @return number of shots fired this frame
     */
    public int update(double dt, List<Enemy> enemies, double damageMultiplier,
                      List<Projectile> outProjectiles) {
        int fired = 0;
        for (Tower tower : towers) {
            tower.update(dt);

            // Re-target when the current target is gone or out of range.
            Enemy current = tower.getTarget();
            boolean targetValid = current != null && current.isAlive()
                    && !current.hasReachedBase()
                    && current.distanceTo(tower.getX(), tower.getY()) <= tower.getRange();
            if (!targetValid) {
                tower.setTarget(tower.findTarget(enemies));
            }

            if (tower.canFire() && tower.getTarget() != null) {
                Projectile p = tower.fire(damageMultiplier);
                if (p != null) {
                    outProjectiles.add(p);
                    fired++;
                }
            }
        }
        return fired;
    }

    /**
     * Notifies every tower about a correct math answer (Math Boost rewards).
     *
     * @return all non-null bonus messages joined for display
     */
    public String notifyCorrectAnswer(com.mathtowerdefense.entities.Player player) {
        StringBuilder messages = new StringBuilder();
        for (Tower tower : towers) {
            String msg = tower.onCorrectAnswer(player);
            if (msg != null) {
                if (messages.length() > 0) {
                    messages.append(" | ");
                }
                messages.append(msg);
            }
        }
        return messages.length() == 0 ? null : messages.toString();
    }

    public List<Tower> getTowers() {
        return Collections.unmodifiableList(towers);
    }

    public Tower getSelected() { return selected; }

    public void clearSelection() { selected = null; }

    public void clear() {
        towers.clear();
        selected = null;
    }

    public int count() { return towers.size(); }
}
