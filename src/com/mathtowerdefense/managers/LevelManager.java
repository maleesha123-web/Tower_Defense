package com.mathtowerdefense.managers;

/**
 * Experience / level progression for the player (spec: Player.level and
 * Player.experience). Pure logic - no GUI, no database.
 */
public final class LevelManager {

    /** XP needed to leave level n: level (n+1) requires n * 200 XP. */
    public static final int BASE_XP_PER_LEVEL = 200;

    /** Maximum level (keeps the curve bounded). */
    public static final int MAX_LEVEL = 20;

    private LevelManager() {}

    /** XP required to advance FROM the given level to the next one. */
    public static int xpRequiredFor(int level) {
        if (level >= MAX_LEVEL) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, level) * BASE_XP_PER_LEVEL;
    }

    /**
     * Adds experience and performs as many level-ups as earned.
     *
     * @param player the player to advance
     * @param xp     experience gained (clamped at 0)
     * @return number of levels gained (0 when none)
     */
    public static int grantExperience(com.mathtowerdefense.entities.Player player, int xp) {
        if (xp <= 0 || player.getLevel() >= MAX_LEVEL) {
            return 0;
        }
        player.addExperience(xp);
        int levelsGained = 0;
        int level = player.getLevel();
        // The player carries a single total-XP counter; level n requires
        // totalXpFor(n) cumulative XP (see totalXpFor).
        while (level < MAX_LEVEL && player.getExperience() >= totalXpFor(level + 1)) {
            level++;
            levelsGained++;
        }
        if (levelsGained > 0) {
            player.setLevel(level);
        }
        return levelsGained;
    }

    /**
     * Total XP required to REACH the given level (level 1 = 0 XP).
     *
     * @param level target level (>= 1)
     */
    public static int totalXpFor(int level) {
        int total = 0;
        for (int l = 1; l < level; l++) {
            total += xpRequiredFor(l);
        }
        return total;
    }
}
