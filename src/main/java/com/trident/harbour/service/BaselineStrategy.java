package com.trident.harbour.service;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.HarbourGraph;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class BaselineStrategy implements RecoveryStrategy {

    /**
     * Conventional dashboard rule: every fault is treated independently and the
     * most damaged asset (severity = 100 - health) is repaired first.
     * Ignores confidence and dependencies. Ties go to the lowest id.
     */
    @Override
    public Optional<Integer> chooseNext(HarbourGraph graph,
                                        Collection<Integer> failedIds,
                                        Map<Integer, Double> confidenceByAsset) {
        Integer best = null;
        double bestSeverity = -1.0;
        for (int id : failedIds) {
            double severity = Asset.MAX_HEALTH - graph.getAsset(id).getHealthScore();
            if (best == null || severity > bestSeverity || (severity == bestSeverity && id < best)) {
                best = id;
                bestSeverity = severity;
            }
        }
        return Optional.ofNullable(best);
    }
}