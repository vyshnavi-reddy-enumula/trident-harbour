package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Ablation: the integrated priority formula WITHOUT dependency awareness.
 * priority = (own criticality x confidence) / repair hours. Ties go to the lowest id.
 */
public class CriticalityPerHourStrategy implements RecoveryStrategy {

    @Override
    public Optional<Integer> chooseNext(HarbourGraph graph,
                                        Collection<Integer> candidates,
                                        Collection<Integer> allUnrepaired,
                                        Map<Integer, Double> confidenceByAsset) {
        Integer best = null;
        double bestPriority = -1.0;
        for (int id : candidates) {
            double confidence = confidenceByAsset.getOrDefault(id, 0.0);
            double p = PriorityScorer.priority(graph.getAsset(id).getCriticality(), confidence,
                    graph.getAsset(id).getRepairHours());
            if (best == null || p > bestPriority || (p == bestPriority && id < best)) {
                best = id;
                bestPriority = p;
            }
        }
        return Optional.ofNullable(best);
    }
}