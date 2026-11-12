package com.mathtowerdefense.managers;

import com.mathtowerdefense.entities.Player;

/**
 * Central scoring rules (spec section 22) + combo bonuses.
 * Kept out of the GUI so the maths can be unit tested and explained.
 */
public final class ScoreManager {

    // ---- flat rewards / penalties ----
    public static final int ENEMY_KILLED = 100;
    public static final int CORRECT_ANSWER = 50;
    public static final int WAVE_COMPLETED = 500;
    public static final int BOSS_KILLED = 1000;
    public static final int WRONG_ANSWER = -25;

    // ---- combo milestones (consecutive correct answers) ----
    public static final int COMBO_3 = 100;
    public static final int COMBO_5 = 250;
    public static final int COMBO_10 = 600;

    private ScoreManager() {}

    /** Applies {@link #ENEMY_KILLED}, doubling nothing but honouring bosses. */
    public static int awardEnemyKill(Player player, boolean boss) {
        int points = boss ? BOSS_KILLED : ENEMY_KILLED;
        player.addScore(points);
        return points;
    }

    /**
     * Applies the correct-answer reward plus any combo milestone bonus.
     *
     * @return total points granted (reward + milestone, 0 if none)
     */
    public static int awardCorrectAnswer(Player player) {
        player.addScore(CORRECT_ANSWER);
        int total = CORRECT_ANSWER;
        int milestone = comboBonusFor(player.getCombo()); // combo already incremented
        if (milestone > 0) {
            player.addScore(milestone);
            total += milestone;
        }
        return total;
    }

    /**
     * Combo bonus for the current streak.
     *
     * @return 100 at exactly 3, 250 at exactly 5, 600 at exactly 10, else 0
     */
    public static int comboBonusFor(int combo) {
        return switch (combo) {
            case 3 -> COMBO_3;
            case 5 -> COMBO_5;
            case 10 -> COMBO_10;
            default -> 0;
        };
    }

    /** Applies the wrong-answer penalty (score never drops below zero). */
    public static int penaliseWrongAnswer(Player player) {
        player.addScore(WRONG_ANSWER); // addScore clamps at 0
        return Math.abs(WRONG_ANSWER);
    }

    /** Applies the wave completion bonus. */
    public static int awardWaveCompleted(Player player) {
        player.addScore(WAVE_COMPLETED);
        return WAVE_COMPLETED;
    }

    /**
     * Difficulty multiplier for enemy stats based on the selected difficulty.
     *
     * @param difficulty 0 = easy, 1 = normal, 2 = hard
     */
    public static double difficultyHealthMultiplier(int difficulty) {
        return switch (Math.max(0, Math.min(difficulty, 2))) {
            case 0 -> 0.8;
            case 2 -> 1.3;
            default -> 1.0;
        };
    }

    /** Accuracy percentage with a division-by-zero guard (for tests + HUD). */
    public static double accuracy(int correct, int incorrect) {
        int total = correct + incorrect;
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(correct * 1000.0 / total) / 10.0;
    }
}
