package com.mathtowerdefense.engine;

/**
 * Session-wide game settings (difficulty + starting resources).
 * A simple singleton so the settings screen and the engine share ONE source
 * of truth without GUI-to-GUI coupling.
 */
public final class GameConfig {

    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_NORMAL = 1;
    public static final int DIFFICULTY_HARD = 2;

    private static final GameConfig INSTANCE = new GameConfig();

    private int difficulty = DIFFICULTY_NORMAL;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true; // resources/sounds/music.wav ships with the game

    private GameConfig() {}

    public static GameConfig getInstance() { return INSTANCE; }

    public int getDifficulty() { return difficulty; }

    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(DIFFICULTY_EASY, Math.min(DIFFICULTY_HARD, difficulty));
    }

    public String getDifficultyLabel() {
        return switch (difficulty) {
            case DIFFICULTY_EASY -> "Easy";
            case DIFFICULTY_HARD -> "Hard";
            default -> "Normal";
        };
    }

    public boolean isSoundEnabled() { return soundEnabled; }

    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public boolean isMusicEnabled() { return musicEnabled; }

    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    /** Starting coins for the current difficulty. */
    public int getStartingCoins() {
        return switch (difficulty) {
            case DIFFICULTY_EASY -> 350;
            case DIFFICULTY_HARD -> 250;
            default -> 300;
        };
    }

    /** Starting base health for the current difficulty. */
    public int getStartingBaseHealth() {
        return switch (difficulty) {
            case DIFFICULTY_EASY -> 150;
            case DIFFICULTY_HARD -> 80;
            default -> 100;
        };
    }
}
