package com.trident.harbour.service;

import com.trident.harbour.demo.AppendixAHarbour;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.RepairTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairSimulatorTest {

    private final HarbourGraph graph = AppendixAHarbour.build();
    private final RepairSimulator simulator = new RepairSimulator();
    private final List<Integer> failed = List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1);
    private final Map<Integer, Double> confidence =
            Map.of(AppendixAHarbour.SUBSTATION_A, 0.7, AppendixAHarbour.PUMP_1, 0.97);

    private void injectFaults() {
        graph.getAsset(AppendixAHarbour.SUBSTATION_A).setHealthScore(20);
        graph.getAsset(AppendixAHarbour.PUMP_1).setHealthScore(0);
    }

    @Test
    void baselineRepairsPumpFirstAndWastesTheEffort() {
        injectFaults();
        List<RepairTask> tasks = simulator.simulate(graph, new BaselineStrategy(), failed, confidence);

        assertEquals(AppendixAHarbour.PUMP_1, tasks.get(0).getAssetId());
        assertTrue(tasks.get(0).isWasted());
        assertEquals(AppendixAHarbour.SUBSTATION_A, tasks.get(1).getAssetId());
        assertFalse(tasks.get(1).isWasted());
        assertEquals(4.0, tasks.get(1).getStartTime(), 0.001);
    }

    @Test
    void integratedRepairsSubstationFirstAndWastesNothing() {
        injectFaults();
        List<RepairTask> tasks = simulator.simulate(graph, new IntegratedStrategy(), failed, confidence);

        assertEquals(AppendixAHarbour.SUBSTATION_A, tasks.get(0).getAssetId());
        assertEquals(AppendixAHarbour.PUMP_1, tasks.get(1).getAssetId());
        assertTrue(tasks.stream().noneMatch(RepairTask::isWasted));
    }

    @Test
    void noFaultsMeansNoTasks() {
        assertTrue(simulator.simulate(graph, new IntegratedStrategy(), List.of(), Map.of()).isEmpty());
    }
}