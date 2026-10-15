package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.RepairTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RepairSimulator {

    /** Single crew. Repairs run back to back, in the order the strategy chooses. */
    public List<RepairTask> simulate(HarbourGraph graph,
                                     RecoveryStrategy strategy,
                                     Collection<Integer> failedIds,
                                     Map<Integer, Double> confidenceByAsset) {
        DependencyAnalyzer analyzer = new DependencyAnalyzer(graph);
        List<Integer> pending = new ArrayList<>(failedIds);
        List<RepairTask> tasks = new ArrayList<>();
        double time = 0.0;

        while (!pending.isEmpty()) {
            int next = strategy.chooseNext(graph, List.copyOf(pending), confidenceByAsset)
                    .orElseThrow(() -> new IllegalStateException(
                            "Strategy returned nothing while faults remain"));
            if (!pending.remove(Integer.valueOf(next))) {
                throw new IllegalStateException("Strategy chose asset " + next + " which is not pending");
            }
            double hours = graph.getAsset(next).getRepairHours();
            // wasted = an upstream fault is still unrepaired, so this asset stays blocked
            boolean wasted = analyzer.findCascade(pending).contains(next);
            tasks.add(new RepairTask(next, 1, time, time + hours, wasted));
            time += hours;
        }
        return tasks;
    }
}