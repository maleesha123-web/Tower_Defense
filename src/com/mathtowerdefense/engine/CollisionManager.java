package com.mathtowerdefense.engine;

import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.Projectile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Steps all projectiles, reports the enemies that were killed this frame
 * and removes expired projectiles. Extracted from GameEngine so collision
 * behaviour is isolated and unit-testable (spec: CollisionManager).
 */
public class CollisionManager {

    /** Explosions created on kill - the renderer turns them into particles. */
    public static final class Explosion {
        public final double x;
        public final double y;
        public final int radius;
        public final Enemy killed;

        Explosion(double x, double y, int radius, Enemy killed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.killed = killed;
        }
    }

    /**
     * Updates every projectile and collects kills.
     *
     * @param projectiles active projectiles (dead ones are removed in place)
     * @param dt          elapsed seconds
     * @return one explosion per enemy killed in this frame
     */
    public List<Explosion> update(List<Projectile> projectiles, double dt) {
        List<Explosion> killed = new ArrayList<>();
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            Enemy target = p.getTarget();
            boolean wasAlive = target != null && target.isAlive();
            boolean hit = p.update(dt);
            if (hit && wasAlive && !target.isAlive()) {
                killed.add(new Explosion(target.getX(), target.getY(),
                        target.getRadius() + 6, target));
            }
            if (!p.isActive()) {
                it.remove();
            }
        }
        return killed;
    }

    /**
     * Pure distance check reused by tests and gameplay logic.
     *
     * @return true when (ax, ay) is within {@code radius} of (bx, by)
     */
    public static boolean withinRange(double ax, double ay, double bx, double by, double radius) {
        return Math.hypot(ax - bx, ay - by) <= radius;
    }

    /**
     * Applies a projectile to a target immediately (used by unit tests to
     * verify damage without running the whole engine).
     *
     * @return true when the target died from this hit
     */
    public static boolean applyHit(Enemy target, int damage) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        return target.takeDamage(damage);
    }
}
