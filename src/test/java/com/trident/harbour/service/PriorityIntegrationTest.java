package com.trident.harbour.service;

import com.trident.harbour.demo.AppendixAHarbour;
import com.trident.harbour.model.EventReport;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityIntegrationTest {

    private final HarbourGraph graph = AppendixAHarbour.build();
    private final DependencyAnalyzer analyzer = new DependencyAnalyzer(graph);
    private final ConfidenceScorer scorer = new ConfidenceScorer();

    private EventReport report(int assetId, SourceType type) {
        return new EventReport(assetId, HazardType.values()[0], type, 3, 0L);
    }

    private double priority(int fixId, List<Integer> failed, double confidence) {
        int unlocked = analyzer.criticalityUnlocked(fixId, failed);
        double hours = graph.getAsset(fixId).getRepairHours();
        return PriorityScorer.priority(unlocked, confidence, hours);
    }

    @Test
    void rootCauseBeatsSymptomEvenWithLowerConfidence() {
        List<Integer> failed = List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1);

        // Substation A: one crew report only (0.7). Pump 1: sensor + crew (0.97).
        double confA = scorer.score(List.of(report(AppendixAHarbour.SUBSTATION_A, SourceType.CREW)));
        double confPump = scorer.score(List.of(
                report(AppendixAHarbour.PUMP_1, SourceType.SENSOR),
                report(AppendixAHarbour.PUMP_1, SourceType.CREW)));

        double pA = priority(AppendixAHarbour.SUBSTATION_A, failed, confA);
        double pPump = priority(AppendixAHarbour.PUMP_1, failed, confPump);

        assertEquals(7.0, pA, 0.001);      // 40 * 0.7 / 4
        assertEquals(0.0, pPump, 0.001);   // fixing the pump first unlocks nothing
        assertTrue(pA > pPump);
    }

    @Test
    void pumpBecomesWorthFixingOnceSubstationIsRepaired() {
        List<Integer> failedAfterSubstationFix = List.of(AppendixAHarbour.PUMP_1);

        double p = priority(AppendixAHarbour.PUMP_1, failedAfterSubstationFix, 0.97);

        assertEquals(2.6675, p, 0.001);    // 11 * 0.97 / 4
    }
}