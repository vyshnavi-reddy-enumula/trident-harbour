package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IntegratedStrategy implements RecoveryStrategy {

    @Override
    public Optional<Integer> chooseNext(HarbourGraph graph,
                                        Collection<Integer> candidates,
                                        Collection<Integer> allUnrepaired,
                                        Map<Integer, Double> confidenceByAsset) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        DependencyAnalyzer analyzer = new DependencyAnalyzer(graph);
        List<Integer> failed = List.copyOf(allUnrepaired);

        Integer best = null;
        double bestPriority = -1.0;
        for (int id : candidates) {
            double confidence = confidenceByAsset.getOrDefault(id, 0.0);
            int unlocked = analyzer.criticalityUnlocked(id, failed);
            double p = PriorityScorer.priority(unlocked, confidence,
                    graph.getAsset(id).getRepairHours());
            // lowest id wins ties, so results are deterministic
            if (best == null || p > bestPriority || (p == bestPriority && id < best)) {
                best = id;
                bestPriority = p;
            }
        }
        return Optional.of(best);
    }
}