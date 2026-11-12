package com.mathtowerdefense.entities;

/**
 * Special tower: grants a coin + score bonus for every correct math answer.
 * This override is the polymorphic "reward hook" of {@link Tower#onCorrectAnswer}.
 */
public class MathBoostTower extends Tower {

    /** Coins awarded to the player per correct answer, per Math Boost tower. */
    public static final int CORRECT_ANSWER_COINS = 15;

    /** Bonus score awarded to the player per correct answer. */
    public static final int CORRECT_ANSWER_SCORE = 25;

    public MathBoostTower(double x, double y) {
        super(TowerType.MATH_BOOST, x, y);
    }

    @Override
    public String onCorrectAnswer(Player player) {
        player.addCoins(CORRECT_ANSWER_COINS);
        player.addScore(CORRECT_ANSWER_SCORE);
        return getDisplayName() + ": +" + CORRECT_ANSWER_COINS + " coins, +"
                + CORRECT_ANSWER_SCORE + " score";
    }
}
