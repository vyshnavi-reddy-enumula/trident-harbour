package com.trident.harbour.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HarbourGraph {

    private final Map<Integer, Asset> assets = new HashMap<>();

    // child ID -> edges to the parents it depends on
    private final Map<Integer, List<Dependency>> dependsOn = new HashMap<>();

    // parent ID -> edges from the children that depend on it
    private final Map<Integer, List<Dependency>> dependents = new HashMap<>();

    public void addAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        int id = asset.getId();
        if (assets.containsKey(id)) {
            throw new DuplicateAssetException(id);
        }
        assets.put(id, asset);
        dependsOn.put(id, new ArrayList<>());
        dependents.put(id, new ArrayList<>());
    }

    /** assetId depends on dependsOnId. */
    public void addDependency(int assetId, int dependsOnId, DependencyType type) {
        checkAssetExists(assetId);
        checkAssetExists(dependsOnId);

        if (type == null) {
            throw new IllegalArgumentException("Dependency type cannot be null");
        }
        if (assetId == dependsOnId) {
            throw new InvalidDependencyException(
                    "An asset cannot depend on itself: " + assetId);
        }
        boolean duplicate = dependsOn.get(assetId).stream()
                .anyMatch(d -> d.getDependsOnId() == dependsOnId);
        if (duplicate) {
            throw new InvalidDependencyException(
                    "Duplicate dependency: " + assetId + " -> " + dependsOnId);
        }
        if (wouldCreateCycle(assetId, dependsOnId)) {
            throw new InvalidDependencyException(
                    "Dependency would create a cycle: "
                            + assetId + " -> " + dependsOnId);
        }

        Dependency edge = new Dependency(assetId, dependsOnId, type);
        dependsOn.get(assetId).add(edge);
        dependents.get(dependsOnId).add(edge);
    }

    /**
     * Adding "child depends on parent" closes a loop if parent already
     * (directly or indirectly) depends on child. Walk upward from parent.
     */
    private boolean wouldCreateCycle(int child, int parent) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(parent);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (current == child) {
                return true;
            }
            if (visited.add(current)) {
                for (Dependency d : dependsOn.get(current)) {
                    stack.push(d.getDependsOnId());
                }
            }
        }
        return false;
    }

    public List<Dependency> getDependencies(int assetId) {
        checkAssetExists(assetId);
        return Collections.unmodifiableList(dependsOn.get(assetId));
    }

    public List<Dependency> getDependents(int assetId) {
        checkAssetExists(assetId);
        return Collections.unmodifiableList(dependents.get(assetId));
    }

    public Asset getAsset(int assetId) {
        checkAssetExists(assetId);
        return assets.get(assetId);
    }

    public Collection<Asset> getAllAssets() {
        return Collections.unmodifiableCollection(assets.values());
    }

    public boolean hasAsset(int assetId) {
        return assets.containsKey(assetId);
    }

    public int assetCount() {
        return assets.size();
    }

    public int dependencyCount() {
        return dependsOn.values().stream().mapToInt(List::size).sum();
    }

    private void checkAssetExists(int assetId) {
        if (!assets.containsKey(assetId)) {
            throw new AssetNotFoundException(assetId);
        }
    }
}