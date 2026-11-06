package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface RecoveryStrategy {

    /**
     * Picks the next asset a free crew should repair.
     *
     * @param candidates        faults no crew has taken yet; the choice comes from here
     * @param allUnrepaired     candidates plus faults other crews are working on;
     *                          all of these are still down (must contain every candidate)
     * @param confidenceByAsset confidence score per asset id; missing means 0
     * @return the asset id to repair, or empty if there are no candidates
     */
    Optional<Integer> chooseNext(HarbourGraph graph,
                                 Collection<Integer> candidates,
                                 Collection<Integer> allUnrepaired,
                                 Map<Integer, Double> confidenceByAsset);

    /** Single-crew convenience: every unrepaired fault is also a candidate. */
    default Optional<Integer> chooseNext(HarbourGraph graph,
                                         Collection<Integer> failedIds,
                                         Map<Integer, Double> confidenceByAsset) {
        return chooseNext(graph, failedIds, failedIds, confidenceByAsset);
    }
}