package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.RepairTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RepairSimulator {

    /** Single crew. Same behaviour as before. */
    public List<RepairTask> simulate(HarbourGraph graph,
                                     RecoveryStrategy strategy,
                                     Collection<Integer> failedIds,
                                     Map<Integer, Double> confidenceByAsset) {
        return simulate(graph, strategy, failedIds, confidenceByAsset, 1);
    }

    /**
     * Event loop. The crew that frees up earliest takes the next fault
     * (ties go to the lowest crew id). Crew ids are 1..crewCount.
     */
    public List<RepairTask> simulate(HarbourGraph graph,
                                     RecoveryStrategy strategy,
                                     Collection<Integer> failedIds,
                                     Map<Integer, Double> confidenceByAsset,
                                     int crewCount) {
        if (crewCount < 1) {
            throw new IllegalArgumentException("crewCount must be at least 1");
        }
        double[] freeAt = new double[crewCount];
        List<Integer> unassigned = new ArrayList<>(failedIds);
        List<RepairTask> draft = new ArrayList<>();

        // Pass 1: decide who repairs what, and when. 'wasted' is not known yet.
        while (!unassigned.isEmpty()) {
            int crew = 0;
            for (int i = 1; i < crewCount; i++) {
                if (freeAt[i] < freeAt[crew]) {
                    crew = i;
                }
            }
            double time = freeAt[crew];

            // still down at 'time': unassigned faults plus repairs not yet finished
            List<Integer> allUnrepaired = new ArrayList<>(unassigned);
            for (RepairTask t : draft) {
                if (t.getEndTime() > time) {
                    allUnrepaired.add(t.getAssetId());
                }
            }

            int next = strategy.chooseNext(graph, List.copyOf(unassigned),
                            List.copyOf(allUnrepaired), confidenceByAsset)
                    .orElseThrow(() -> new IllegalStateException(
                            "Strategy returned nothing while faults remain"));
            if (!unassigned.remove(Integer.valueOf(next))) {
                throw new IllegalStateException("Strategy chose asset " + next + " which is not unassigned");
            }

            double hours = graph.getAsset(next).getRepairHours();
            draft.add(new RepairTask(next, crew + 1, time, time + hours, false));
            freeAt[crew] = time + hours;
        }

        // Pass 2: now every end time is known, so 'wasted' can be decided.
        // A task ending at e is wasted if its asset is still blocked by a fault
        // whose own repair ends after e. A fault ending exactly at e counts as repaired.
        DependencyAnalyzer analyzer = new DependencyAnalyzer(graph);
        List<RepairTask> result = new ArrayList<>();
        for (RepairTask t : draft) {
            List<Integer> stillDown = new ArrayList<>();
            for (RepairTask other : draft) {
                if (other.getEndTime() > t.getEndTime()) {
                    stillDown.add(other.getAssetId());
                }
            }
            boolean wasted = analyzer.findCascade(stillDown).contains(t.getAssetId());
            result.add(new RepairTask(t.getAssetId(), t.getCrewId(),
                    t.getStartTime(), t.getEndTime(), wasted));
        }
        return result;
    }
}