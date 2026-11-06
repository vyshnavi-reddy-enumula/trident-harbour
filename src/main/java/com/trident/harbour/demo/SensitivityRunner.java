package com.trident.harbour.demo;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.RepairTask;
import com.trident.harbour.service.*;
import com.trident.harbour.sim.Scenario;
import com.trident.harbour.sim.ScenarioGenerator;
import com.trident.harbour.sim.Severity;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sensitivity of the ablation to the assumed Main Grid repair time (asset 1, default 8 h).
 * Rules (declared before running): power-involving configurations only, MEDIUM and HIGH, 1 crew,
 * weather 1.2, seeds 1..100, skip seeds with < 2 faults, Main Grid repair hours in {3, 5, 8, 12}.
 * Faults do not depend on repair hours, so every hours value sees identical faults.
 * Columns: mean weighted downtime of baseline / rule-only / integrated and their % improvement over baseline.
 */
public final class SensitivityRunner {

    private static final double[] MAIN_GRID_HOURS = {3.0, 5.0, 8.0, 12.0};

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        List<List<HazardType>> combos = new ArrayList<>();
        combos.add(List.of(HazardType.POWER_OUTAGE));
        combos.add(List.of(HazardType.FLOOD, HazardType.POWER_OUTAGE));
        combos.add(List.of(HazardType.POWER_OUTAGE, HazardType.EQUIPMENT_FAILURE));

        ScenarioGenerator gen = new ScenarioGenerator();
        RepairSimulator sim = new RepairSimulator();
        RecoveryStrategy[] strategies = {new BaselineStrategy(), new CriticalityPerHourStrategy(), new IntegratedStrategy()};

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of("results_sensitivity.csv")))) {
            out.println("scenario,severity,main_grid_hours,n,wd_base,wd_rule,wd_int,rule_impr_pct,int_impr_pct");
            System.out.printf("%-28s %-6s %5s %4s | %8s %8s %8s | %9s %9s%n",
                    "scenario", "sev", "MGh", "n", "wdBase", "wdRule", "wdInt", "rule%", "int%");
            for (List<HazardType> combo : combos) {
                String name = combo.stream().map(Enum::name).collect(Collectors.joining("+"));
                for (Severity sev : new Severity[]{Severity.MEDIUM, Severity.HIGH}) {
                    for (double mg : MAIN_GRID_HOURS) {
                        double[] wd = new double[3];
                        int n = 0;
                        for (long seed = 1; seed <= 100; seed++) {
                            HarbourGraph g = AppendixAHarbour.build(Map.of(1, mg));
                            List<Integer> faults = gen.injectFaults(g, new Scenario(combo, sev, 1.2, seed));
                            if (faults.size() < 2) continue;
                            Map<Integer, Double> conf = new HashMap<>();
                            for (int id : faults) conf.put(id, 1.0);
                            n++;
                            for (int s = 0; s < 3; s++) {
                                List<RepairTask> t = sim.simulate(g, strategies[s], faults, conf, 1);
                                wd[s] += new MetricsCalculator(g).weightedDowntime(t);
                            }
                        }
                        if (n == 0) continue;
                        double b = wd[0] / n, r = wd[1] / n, i = wd[2] / n;
                        double rp = (b - r) / b * 100.0, ip = (b - i) / b * 100.0;
                        System.out.printf("%-28s %-6s %5.0f %4d | %8.1f %8.1f %8.1f | %9.1f %9.1f%n",
                                name, sev, mg, n, b, r, i, rp, ip);
                        out.printf("%s,%s,%.0f,%d,%.2f,%.2f,%.2f,%.1f,%.1f%n", name, sev, mg, n, b, r, i, rp, ip);
                    }
                }
            }
        }
    }
}