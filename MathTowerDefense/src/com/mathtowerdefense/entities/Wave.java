package com.mathtowerdefense.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of one enemy wave: the ordered composition of enemies
 * plus the difficulty multipliers applied to their base stats.
 * WaveManager builds and schedules these.
 */
public class Wave {

    private final int number;
    private final List<EnemyType> composition; // spawn order
    private final double healthMultiplier;
    private final double speedMultiplier;
    private final long spawnIntervalMs;        // delay between spawns

    public Wave(int number, List<EnemyType> composition,
                double healthMultiplier, double speedMultiplier, long spawnIntervalMs) {
        if (number < 1) {
            throw new IllegalArgumentException("Wave number must be >= 1.");
        }
        if (composition == null || composition.isEmpty()) {
            throw new IllegalArgumentException("Wave needs at least one enemy.");
        }
        this.number = number;
        this.composition = new ArrayList<>(composition);
        this.healthMultiplier = Math.max(0.1, healthMultiplier);
        this.speedMultiplier = Math.max(0.1, speedMultiplier);
        this.spawnIntervalMs = Math.max(100, spawnIntervalMs);
    }

    public int getNumber() { return number; }

    /** Unmodifiable spawn order. */
    public List<EnemyType> getComposition() {
        return Collections.unmodifiableList(composition);
    }

    public int getEnemyCount() { return composition.size(); }

    public EnemyType getTypeAt(int index) { return composition.get(index); }

    public double getHealthMultiplier() { return healthMultiplier; }

    public double getSpeedMultiplier() { return speedMultiplier; }

    public long getSpawnIntervalMs() { return spawnIntervalMs; }

    public boolean isBossWave() { return composition.contains(EnemyType.BOSS); }
}
