package com.trident.harbour.model;

/**
 * One edge in the harbour graph: assetId depends on dependsOnId.
 * In getDependents(x), dependsOnId is x and assetId is the child.
 */
public class Dependency {
    private final int assetId;
    private final int dependsOnId;
    private final DependencyType type;

    public Dependency(int assetId, int dependsOnId, DependencyType type) {
        this.assetId = assetId;
        this.dependsOnId = dependsOnId;
        this.type = type;
    }

    public int getAssetId() { return assetId; }
    public int getDependsOnId() { return dependsOnId; }
    public DependencyType getType() { return type; }

    @Override
    public String toString() {
        return assetId + " -> " + dependsOnId + " (" + type + ")";
    }
}