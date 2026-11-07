package com.mathtowerdefense.engine;

/**
 * The states a game can be in. {@code GameEngine} owns every transition.
 *
 * MENU           - main menu (engine idle)
 * PLAYING        - simulation running
 * PAUSED         - simulation frozen, world preserved
 * MATH_CHALLENGE - world frozen while a question is shown (timer running)
 * GAME_OVER      - base destroyed (terminal)
 * VICTORY        - all waves cleared (terminal)
 */
public enum GameState {
    MENU,
    PLAYING,
    PAUSED,
    MATH_CHALLENGE,
    GAME_OVER,
    VICTORY
}
