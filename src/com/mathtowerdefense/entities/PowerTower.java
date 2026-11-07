package com.mathtowerdefense.entities;

/** Expensive, slow-firing, high-damage tower. */
public class PowerTower extends Tower {
    public PowerTower(double x, double y) {
        super(TowerType.POWER, x, y);
    }
}
