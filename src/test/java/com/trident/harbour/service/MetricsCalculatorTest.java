package com.trident.harbour.service;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.RepairTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.trident.harbour.demo.AppendixAHarbour;   

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsCalculatorTest {

    private HarbourGraph graph;
    private int substationA;
    private int pump1;

    @BeforeEach
    void setUp() {
         graph = AppendixAHarbour.build();
        substationA = AppendixAHarbour.SUBSTATION_A;
        pump1 = AppendixAHarbour.PUMP_1;
    }

    @Test
    void integratedOrderDowntime() {
        List<RepairTask> tasks = List.of(
            new RepairTask(substationA, 1, 0.0, 4.0, false),
            new RepairTask(pump1,       1, 4.0, 8.0, false));
        MetricsCalculator m = new MetricsCalculator(graph);
        assertEquals(248.0, m.weightedDowntime(tasks), 1e-9);
        assertEquals(0.0, m.wastedRepairHours(tasks), 1e-9);
    }

    @Test
    void baselineOrderDowntime() {
        List<RepairTask> tasks = List.of(
            new RepairTask(pump1,       1, 0.0, 4.0, true),
            new RepairTask(substationA, 1, 4.0, 8.0, false));
        MetricsCalculator m = new MetricsCalculator(graph);
        assertEquals(408.0, m.weightedDowntime(tasks), 1e-9);
        assertEquals(4.0, m.wastedRepairHours(tasks), 1e-9);
    }

    @Test
    void doubleFaultBothFinishAtHour8() {
         List<RepairTask> integrated = List.of(
            new RepairTask(substationA, 1, 0.0, 4.0, false),
            new RepairTask(pump1,       1, 4.0, 8.0, false));
        List<RepairTask> baseline = List.of(
                new RepairTask(pump1,       1, 0.0, 4.0, true),
            new RepairTask(substationA, 1, 4.0, 8.0, false));
        MetricsCalculator m = new MetricsCalculator(graph);
        assertEquals(8.0, m.timeToRestoreCritical(integrated), 1e-9);
        assertEquals(8.0, m.timeToRestoreCritical(baseline), 1e-9);
}

    @Test
    void lowCriticalityFaultLeftLastRestoresEarly() {
        int access = AppendixAHarbour.ACCESS_ROAD_GATE; // check the real constant name
        List<RepairTask> integrated = List.of(
            new RepairTask(substationA, 1, 0.0, 4.0, false),
           new RepairTask(access,      1, 4.0, 8.0, false));
       List<RepairTask> baseline = List.of(
          new RepairTask(access,      1, 0.0, 4.0, true),
          new RepairTask(substationA, 1, 4.0, 8.0, false));
        MetricsCalculator m = new MetricsCalculator(graph);
        assertEquals(4.0, m.timeToRestoreCritical(integrated), 1e-9);
        assertEquals(8.0, m.timeToRestoreCritical(baseline), 1e-9);
}
}