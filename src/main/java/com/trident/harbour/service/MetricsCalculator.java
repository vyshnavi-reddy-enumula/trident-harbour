package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.RepairTask;

import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

public class MetricsCalculator {

    private final HarbourGraph graph;
    private final DependencyAnalyzer analyzer;

    public MetricsCalculator(HarbourGraph graph) {
        this.graph = graph;
        this.analyzer = new DependencyAnalyzer(graph);
    }

    /** Sum over assets of criticality x hours not operational, from t = 0. */
    public double weightedDowntime(List<RepairTask> tasks) {
        List<Double> boundaries = new ArrayList<>();
        boundaries.add(0.0);
        for (RepairTask t : tasks) {
            boundaries.add(t.getEndTime());
        }
        List<Double> times = boundaries.stream().distinct().sorted().toList();

        double total = 0.0;
        for (int i = 0; i < times.size() - 1; i++) {
            final double start = times.get(i);
            double length = times.get(i + 1) - start;
            // faults still unrepaired during this interval
            List<Integer> unrepaired = tasks.stream()
                    .filter(t -> t.getEndTime() > start)
                    .map(RepairTask::getAssetId)
                    .toList();

        Set<Integer> down = new HashSet<>(unrepaired);
        down.addAll(analyzer.findCascade(unrepaired));
        for (int id : down) {
        total += graph.getAsset(id).getCriticality() * length;
        }

        }
        return total;
    }

    /** Crew-hours spent on repairs that could not make the asset operational. */
    public double wastedRepairHours(List<RepairTask> tasks) {
        double total = 0.0;
        for (RepairTask t : tasks) {
            if (t.isWasted()) {
                total += t.getHours();
            }
        }
        return total;
    }


public static final int CRITICAL_THRESHOLD = 4;

private Set<Integer> downAssets(List<Integer> unrepaired) {
    Set<Integer> down = new HashSet<>(unrepaired);
    down.addAll(analyzer.findCascade(unrepaired));
    return down;
}

private boolean criticalDown(List<Integer> unrepaired) {
    for (int id : downAssets(unrepaired)) {
        if (graph.getAsset(id).getCriticality() >= CRITICAL_THRESHOLD) {
            return true;
        }
    }
    return false;
}

/** First moment at which no asset with criticality >= 4 is down. */
public double timeToRestoreCritical(List<RepairTask> tasks) {
    List<Double> times = new ArrayList<>();
    times.add(0.0);
    for (RepairTask t : tasks) {
        times.add(t.getEndTime());
    }
    for (double t : times.stream().distinct().sorted().toList()) {
        List<Integer> unrepaired = tasks.stream()
                .filter(x -> x.getEndTime() > t)
                .map(RepairTask::getAssetId)
                .toList();
        if (!criticalDown(unrepaired)) {
            return t;
        }
    }
    return 0.0;
}
}