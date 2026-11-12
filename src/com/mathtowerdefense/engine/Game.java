package com.mathtowerdefense.engine;

import com.mathtowerdefense.entities.Player;

/**
 * Composition root of a single play session: owns the {@link GameEngine}
 * and its {@link GameLoop} so the GUI talks to ONE object per game.
 *
 * Typical usage from the game screen:
 * <pre>
 *   game = new Game(username, difficulty, listener, this::repaint);
 *   game.start();          // begins the Swing timer
 *   ...
 *   game.stop();           // on pause / leaving the screen
 * </pre>
 */
public class Game {

    private final GameEngine engine;
    private final GameLoop loop;

    public Game(String username, int difficulty, GameEngine.Listener listener, Runnable onFrame) {
        this.engine = new GameEngine(username, difficulty, listener);
        this.loop = new GameLoop(engine, onFrame);
    }

    public GameEngine getEngine() {
        return engine;
    }

    /** Starts/resumes the frame loop (idempotent). */
    public void start() {
        loop.start();
    }

    /** Stops the frame loop; the world keeps its state (pause/restart). */
    public void stop() {
        loop.stop();
    }

    public boolean isRunning() {
        return loop.isRunning();
    }

    /** Resets the world for a fresh run and restarts the loop. */
    public void restart() {
        engine.startNewGame();
        loop.start();
    }

    public Player getPlayer() {
        return engine.getPlayer();
    }
}
