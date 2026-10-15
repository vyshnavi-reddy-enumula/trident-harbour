package com.trident.harbour.service;

import com.trident.harbour.model.EventReport;
import com.trident.harbour.model.SourceType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ConfidenceScorer {

    /**
     * Noisy-OR corroboration: confidence = 1 - product(1 - reliability).
     * Only one report per source type counts, so repeated reports
     * from the same source type cannot inflate the score.
     */
    public double score(List<EventReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return 0.0;
        }
        Set<SourceType> sources = EnumSet.noneOf(SourceType.class);
        for (EventReport r : reports) {
            sources.add(r.getSourceType());
        }
        double allWrong = 1.0;
        for (SourceType s : sources) {
            allWrong *= (1.0 - s.getReliability());
        }
        return 1.0 - allWrong;
    }
}