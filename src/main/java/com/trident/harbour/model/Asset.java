package com.trident.harbour.model;

import com.trident.harbour.exception.IllegalAssetStateException;
import com.trident.harbour.exception.InvalidAssetException;
import com.trident.harbour.exception.InvalidHealthException;

import java.util.Objects;

/**
 * Base class for every harbour asset.
 *
 * Encapsulation: health and repair state can only change through the methods below,
 * so status can never contradict health.
 * Polymorphism: each subclass supplies its own failure threshold, hazard vulnerability,
 * type and service description; the simulator only ever talks to Asset.
 *
 * NOTE: status is DERIVED from health + repair flag, never stored separately.
 */
public abstract class Asset {

    public static final double MAX_HEALTH = 100.0;
    private static final double DEGRADED_THRESHOLD = 70.0;

    private final int id;
    private final String name;
    private final int criticality;      // 1 (low) .. 5 (highest)
    private final double repairHours;   // fixed repair time for this asset
    private double healthScore;         // 0..100
    private boolean underRepair;

    protected Asset(int id, String name, int criticality, double repairHours, double healthScore) {
        if (name == null || name.isBlank()) {
            throw new InvalidAssetException("Asset name must not be blank");
        }
        if (criticality < 1 || criticality > 5) {
            throw new InvalidAssetException("Criticality must be 1..5 but was " + criticality);
        }
        if (repairHours <= 0) {
            throw new InvalidAssetException("Repair hours must be positive but was " + repairHours);
        }
        this.id = id;
        this.name = name;
        this.criticality = criticality;
        this.repairHours = repairHours;
        this.healthScore = checkHealth(healthScore);
    }

    // ---- polymorphic behaviour (each subclass decides) ----

    public abstract AssetType getType();

    public abstract String getServiceProvided();

    /** Health below this value means the asset has failed. Assumed value: justify/sensitivity-test it. */
    public abstract double getFailureThreshold();

    /** 0..1: how strongly this kind of asset is hit by a hazard. Assumed value: sensitivity-test it. */
    public abstract double getVulnerability(HazardType hazard);

    // ---- state changes (the only way to touch health) ----

    public void setHealthScore(double value) {
        this.healthScore = checkHealth(value);
    }

    public void applyDamage(double amount) {
        if (amount < 0 || Double.isNaN(amount)) {
            throw new InvalidAssetException("Damage must be non-negative but was " + amount);
        }
        this.healthScore = Math.max(0.0, healthScore - amount);
    }

    public void startRepair() {
        if (underRepair) {
            throw new IllegalAssetStateException(name + " is already under repair");
        }
        if (healthScore >= MAX_HEALTH) {
            throw new IllegalAssetStateException(name + " is fully healthy; nothing to repair");
        }
        this.underRepair = true;
    }

    public void completeRepair() {
        if (!underRepair) {
            throw new IllegalAssetStateException(name + " has no repair in progress");
        }
        this.underRepair = false;
        this.healthScore = MAX_HEALTH;
    }

    // ---- queries ----

    public AssetStatus getStatus() {
        if (underRepair) return AssetStatus.UNDER_REPAIR;
        if (healthScore < getFailureThreshold()) return AssetStatus.FAILED;
        if (healthScore < DEGRADED_THRESHOLD) return AssetStatus.DEGRADED;
        return AssetStatus.OPERATIONAL;
    }

    /** Own-fault view only. Whether an upstream failure blocks it is the Dependency Analyzer's job. */
    public boolean isOperational() {
        AssetStatus s = getStatus();
        return s == AssetStatus.OPERATIONAL || s == AssetStatus.DEGRADED;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCriticality() { return criticality; }
    public double getRepairHours() { return repairHours; }
    public double getHealthScore() { return healthScore; }

    private static double checkHealth(double v) {
        if (Double.isNaN(v) || v < 0 || v > MAX_HEALTH) {
            throw new InvalidHealthException(v);
        }
        return v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getType() + "#" + id + " " + name + " [health=" + healthScore + ", " + getStatus() + "]";
    }
}
