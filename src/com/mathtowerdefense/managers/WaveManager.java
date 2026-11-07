package com.mathtowerdefense.managers;

import com.mathtowerdefense.entities.EnemyType;
import com.mathtowerdefense.entities.Wave;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the wave schedule and drives spawning/completion detection.
 *
 * Progression rules (spec section 11):
 *   - wave 1: 5 enemies, wave 2: 7, wave 3: 10, wave 4: 12, wave 5: boss ...
 *   - health, speed and density grow every wave; a boss appears on every
 *     5th wave; the campaign ends after {@link #TOTAL_WAVES}.
 *
 * The class is deliberately GUI-free so progression can be unit tested.
 */
public class WaveManager {

    public static final int TOTAL_WAVES = 10;

    private final List<Wave> waves = new ArrayList<>();
    private int currentIndex = -1;      // -1 = campaign not started
    private int spawnedInWave = 0;      // how many enemies of the current wave exist
    private double spawnTimer;          // seconds until the next spawn
    private boolean waveFinished;       // current wave fully spawned AND cleared
    private boolean completed;          // all waves done

    /** Builds the full schedule once (deterministic, testable). */
    public WaveManager() {
        for (int n = 1; n <= TOTAL_WAVES; n++) {
            waves.add(buildWave(n));
        }
    }

    /** Creates the composition/difficulty of wave {@code n}. */
    static Wave buildWave(int n) {
        List<EnemyType> composition = new ArrayList<>();

        if (n % 5 == 0) {
            // Boss wave: escort pack + boss.
            int escorts = 6 + n;
            for (int i = 0; i < escorts; i++) {
                composition.add(i % 3 == 0 ? EnemyType.FAST : EnemyType.NORMAL);
            }
            composition.add(EnemyType.BOSS);
        } else {
            int count = switch (n) {
                case 1 -> 5;
                case 2 -> 7;
                case 3 -> 10;
                case 4 -> 12;
                default -> 12 + (n - 4) * 3; // waves 6+ keep growing
            };
            for (int i = 0; i < count; i++) {
                if (n >= 3 && i % 4 == 3) {
                    composition.add(EnemyType.TANK);
                } else if (n >= 2 && i % 3 == 2) {
                    composition.add(EnemyType.FAST);
                } else {
                    composition.add(EnemyType.NORMAL);
                }
            }
        }

        double healthMult = 1.0 + 0.20 * (n - 1);   // +20 % health per wave
        double speedMult = Math.min(1.8, 1.0 + 0.05 * (n - 1));
        long interval = Math.max(400, 1100 - 70L * n);
        return new Wave(n, composition, healthMult, speedMult, interval);
    }
    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Starts wave 1. */
    public void startCampaign() {
        currentIndex = -1;
        spawnedInWave = 0;
        spawnTimer = 0;
        waveFinished = false;
        completed = false;
        startNextWave();
    }

    /**
     * Advances to the next wave.
     *
     * @return true when a new wave actually started
     */
    public boolean startNextWave() {
        if (currentIndex + 1 >= waves.size()) {
            completed = true;
            return false;
        }
        currentIndex++;
        spawnedInWave = 0;
        spawnTimer = 0;
        waveFinished = false;
        return true;
    }

    /**
     * Spawns enemies of the current wave over time.
     *
     * @param dt elapsed seconds since the previous frame
     * @return the enemy type to spawn, or {@code null} when nothing is due
     */
    public EnemyType pollSpawn(double dt) {
        if (!isWaveActive() || waveFinished) {
            return null;
        }
        Wave wave = currentWave();
        if (spawnedInWave >= wave.getEnemyCount()) {
            return null;
        }
        spawnTimer -= dt;
        if (spawnTimer > 0) {
            return null;
        }
        EnemyType next = wave.getTypeAt(spawnedInWave);
        spawnedInWave++;
        spawnTimer = wave.getSpawnIntervalMs() / 1000.0;
        return next;
    }

    /**
     * Marks the current wave complete once every enemy spawned and none is
     * alive anymore.
     *
     * @param enemiesAlive number of living enemies on the map
     * @return true exactly once per wave when the wave was just cleared
     */
    public boolean checkWaveCleared(int enemiesAlive) {
        if (!isWaveActive() || waveFinished || enemiesAlive > 0) {
            return false;
        }
        Wave wave = currentWave();
        if (spawnedInWave < wave.getEnemyCount()) {
            return false;
        }
        waveFinished = true;
        return true;
    }

    /** True while the campaign still has unplayed waves. */
    public boolean hasNextWave() {
        return currentIndex + 1 < waves.size();
    }

    /** True when a wave is currently running (spawned or spawning). */
    public boolean isWaveActive() {
        return currentIndex >= 0 && currentIndex < waves.size();
    }

    public boolean isCompleted() { return completed; }

    /** 1-based number of the current wave (0 before the campaign starts). */
    public int getCurrentWaveNumber() {
        return currentIndex < 0 ? 0 : currentIndex + 1;
    }

    public Wave currentWave() {
        return currentIndex < 0 || currentIndex >= waves.size()
                ? null : waves.get(currentIndex);
    }

    public Wave waveAt(int zeroBased) {
        return zeroBased < 0 || zeroBased >= waves.size() ? null : waves.get(zeroBased);
    }

    public int getTotalWaves() { return waves.size(); }

    /** Enemies of the current wave that have not been spawned yet. */
    public int getRemainingToSpawn() {
        Wave w = currentWave();
        if (w == null || waveFinished) {
            return 0;
        }
        return Math.max(0, w.getEnemyCount() - spawnedInWave);
    }

    /** True once the current wave produced all of its enemies. */
    public boolean isCurrentWaveFullySpawned() {
        Wave w = currentWave();
        return w != null && spawnedInWave >= w.getEnemyCount();
    }

    /** Resets all state (used when restarting a game). */
    public void reset() {
        currentIndex = -1;
        spawnedInWave = 0;
        spawnTimer = 0;
        waveFinished = false;
        completed = false;
    }
}
