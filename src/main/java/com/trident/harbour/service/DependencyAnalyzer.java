package com.trident.harbour.service;

import com.trident.harbour.model.Dependency;
import com.trident.harbour.model.DependencyType;
import com.trident.harbour.model.HarbourGraph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DependencyAnalyzer {

    private final HarbourGraph graph;

    public DependencyAnalyzer(HarbourGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns the ids of assets knocked out because an upstream asset failed.
     * Follows REQUIRED links only, using BFS. The originally failed assets
     * are not included in the result.
     */
    public Set<Integer> findCascade(Collection<Integer> failedIds) {
        for (int id : failedIds) {
            graph.getAsset(id);   // throws AssetNotFoundException for unknown ids
        }

        Set<Integer> visited = new HashSet<>(failedIds);
        Deque<Integer> queue = new ArrayDeque<>(failedIds);
        Set<Integer> blocked = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (Dependency d : graph.getDependents(current)) {
                if (d.getType() != DependencyType.REQUIRED) {
                    continue;
                }
                int child = d.getAssetId();
                if (visited.add(child)) {
                    blocked.add(child);
                    queue.add(child);
                }
            }
        }
        return blocked;
    }

    /**
     * Total criticality that comes back if fixId is repaired, given the
     * assets that are currently failed. Fixing something whose downstream
     * assets are still blocked by another fault unlocks nothing.
     */
    public int criticalityUnlocked(int fixId, Collection<Integer> failedIds) {
        if (!failedIds.contains(fixId)) {
            throw new IllegalArgumentException(
                    "Asset " + fixId + " is not one of the failed assets");
        }
        Set<Integer> before = blockedSet(failedIds);

        List<Integer> remaining = failedIds.stream()
                .filter(id -> id != fixId)
                .toList();
        Set<Integer> after = blockedSet(remaining);

        before.removeAll(after);   // what the fix brings back
        return before.stream()
                .mapToInt(id -> graph.getAsset(id).getCriticality())
                .sum();
    }

    /** Failed assets plus everything they knock out. */
    private Set<Integer> blockedSet(Collection<Integer> failedIds) {
        Set<Integer> blocked = new HashSet<>(failedIds);
        blocked.addAll(findCascade(failedIds));
        return blocked;
    }
}