package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface RecoveryStrategy {

    /**
     * Picks the next asset a free crew should repair.
     *
     * @param failedIds         assets with their own real fault (not knock-on effects)
     * @param confidenceByAsset confidence score per asset id; missing means 0
     * @return the asset id to repair, or empty if nothing is left to repair
     */
    Optional<Integer> chooseNext(HarbourGraph graph,
                                 Collection<Integer> failedIds,
                                 Map<Integer, Double> confidenceByAsset);
}