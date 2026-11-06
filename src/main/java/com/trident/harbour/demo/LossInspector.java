package com.trident.harbour.demo;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.RepairTask;
import com.trident.harbour.service.BaselineStrategy;
import com.trident.harbour.service.IntegratedStrategy;
import com.trident.harbour.service.MetricsCalculator;
import com.trident.harbour.service.RepairSimulator;
import com.trident.harbour.sim.Scenario;
import com.trident.harbour.sim.ScenarioGenerator;
import com.trident.harbour.sim.Severity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Prints every seed where integrated has WORSE weighted downtime than baseline. */
public final class LossInspector {

    // change these three lines to inspect another row of the table
    private static final List<HazardType> COMBO = List.of(HazardType.POWER_OUTAGE);
    private static final Severity SEV = Severity.HIGH;
    private static final int CREWS = 1;

    public static void main(String[] args) {
        ScenarioGenerator gen = new ScenarioGenerator();
        RepairSimulator sim = new RepairSimulator();
        for (long seed = 1; seed <= 100; seed++) {
            HarbourGraph g = AppendixAHarbour.build();
            List<Integer> faults = gen.injectFaults(g, new Scenario(COMBO, SEV, 1.2, seed));
            if (faults.size() < 2) {
                continue;
            }
            Map<Integer, Double> conf = new HashMap<>();
            for (int id : faults) {
                conf.put(id, 1.0);
            }
            List<RepairTask> b = sim.simulate(g, new BaselineStrategy(), faults, conf, CREWS);
            List<RepairTask> i = sim.simulate(g, new IntegratedStrategy(), faults, conf, CREWS);
            MetricsCalculator m = new MetricsCalculator(g);
            double db = m.weightedDowntime(b);
            double di = m.weightedDowntime(i);
            if (di > db + 1e-9) {
                System.out.printf("seed %d faults=%s  base=%.1f int=%.1f%n  base order: %s%n  int  order: %s%n%n",
                        seed, faults, db, di, order(b), order(i));
            }
        }
    }

    private static String order(List<RepairTask> tasks) {
        StringBuilder sb = new StringBuilder();
        for (RepairTask t : tasks) {
            sb.append(t.getAssetId()).append('@').append(t.getEndTime()).append(' ');
        }
        return sb.toString();
    }
}