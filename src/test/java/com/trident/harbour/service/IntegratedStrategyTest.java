package com.trident.harbour.service;

import com.trident.harbour.demo.AppendixAHarbour;
import com.trident.harbour.model.HarbourGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegratedStrategyTest {

    private final HarbourGraph graph = AppendixAHarbour.build();
    private final RecoveryStrategy strategy = new IntegratedStrategy();

    @Test
    void picksRootCauseOverHigherConfidenceSymptom() {
        Optional<Integer> next = strategy.chooseNext(graph,
                List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1),
                Map.of(AppendixAHarbour.SUBSTATION_A, 0.7, AppendixAHarbour.PUMP_1, 0.97));
        assertEquals(Optional.of(AppendixAHarbour.SUBSTATION_A), next);
    }

    @Test
    void picksPumpWhenItIsTheOnlyFault() {
        Optional<Integer> next = strategy.chooseNext(graph,
                List.of(AppendixAHarbour.PUMP_1),
                Map.of(AppendixAHarbour.PUMP_1, 0.97));
        assertEquals(Optional.of(AppendixAHarbour.PUMP_1), next);
    }

    @Test
    void returnsEmptyWhenNothingFailed() {
        assertEquals(Optional.empty(), strategy.chooseNext(graph, List.of(), Map.of()));
    }
}