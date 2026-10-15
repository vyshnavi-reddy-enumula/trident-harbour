package com.trident.harbour.service;

import com.trident.harbour.demo.AppendixAHarbour;
import com.trident.harbour.model.HarbourGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaselineStrategyTest {

    private final HarbourGraph graph = AppendixAHarbour.build();
    private final RecoveryStrategy baseline = new BaselineStrategy();
    private final RecoveryStrategy integrated = new IntegratedStrategy();

    @Test
    void picksMostDamagedAssetEvenWhenItIsASymptom() {
        graph.getAsset(AppendixAHarbour.SUBSTATION_A).setHealthScore(20);
        graph.getAsset(AppendixAHarbour.PUMP_1).setHealthScore(0);
        List<Integer> failed = List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1);
        Map<Integer, Double> confidence = Map.of(
                AppendixAHarbour.SUBSTATION_A, 0.7, AppendixAHarbour.PUMP_1, 0.97);

        assertEquals(Optional.of(AppendixAHarbour.PUMP_1),
                baseline.chooseNext(graph, failed, confidence));
        assertEquals(Optional.of(AppendixAHarbour.SUBSTATION_A),
                integrated.chooseNext(graph, failed, confidence));
    }

    @Test
    void tieGoesToLowestId() {
        graph.getAsset(AppendixAHarbour.SUBSTATION_A).setHealthScore(0);
        graph.getAsset(AppendixAHarbour.PUMP_1).setHealthScore(0);
        assertEquals(Optional.of(AppendixAHarbour.SUBSTATION_A),
                baseline.chooseNext(graph,
                        List.of(AppendixAHarbour.PUMP_1, AppendixAHarbour.SUBSTATION_A), Map.of()));
    }

    @Test
    void returnsEmptyWhenNothingFailed() {
        assertEquals(Optional.empty(), baseline.chooseNext(graph, List.of(), Map.of()));
    }
}