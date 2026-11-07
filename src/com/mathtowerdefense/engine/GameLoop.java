package com.mathtowerdefense.engine;

import javax.swing.Timer;

/**
 * Fixed-timestep game loop built on a Swing Timer (~60 updates per second).
 *
 * The Timer fires on the Event Dispatch Thread, which keeps all game state
 * single-threaded (no locking needed for Swing rendering) and - unlike a bare
 * while(true) loop - never burns 100% CPU (spec section 27).
 */
public class GameLoop {

    /** Target frame time in milliseconds (~16 ms = 60 FPS). */
    public static final int FRAME_MS = 16;

    private final GameEngine engine;
    private final Runnable onFrame; // repaint callback (GUI)
    private Timer timer;
    private long lastFrameNanos;

    public GameLoop(GameEngine engine, Runnable onFrame) {
        if (engine == null || onFrame == null) {
            throw new IllegalArgumentException("GameLoop needs an engine and a frame callback.");
        }
        this.engine = engine;
        this.onFrame = onFrame;
    }

    /** Starts the loop (idempotent). */
    public synchronized void start() {
        if (isRunning()) {
            return;
        }
        lastFrameNanos = System.nanoTime();
        timer = new Timer(FRAME_MS, e -> tick());
        timer.start();
    }

    /** Stops the loop; the world state stays as-is (pause/restart use this). */
    public synchronized void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public synchronized boolean isRunning() {
        return timer != null && timer.isRunning();
    }

    /** One iteration: compute dt (clamped), update the engine, then render. */
    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        // Clamp dt so a long pause (breakpoint, window drag) cannot teleport
        // enemies across the map.
        dt = Math.min(dt, 0.1);

        engine.update(dt);
        onFrame.run();
    }
}
