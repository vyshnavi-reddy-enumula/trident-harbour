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

/** Same rules as ExperimentRunner (MEDIUM/HIGH, weather 1.2, seeds 1..100, skip < 2 faults), three strategies. */
public final class AblationRunner {

    private static final int MIN_FAULTS = 2;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        HazardType[] hz = HazardType.values();
        List<List<HazardType>> combos = new ArrayList<>();
        for (HazardType h : hz) combos.add(List.of(h));
        for (int i = 0; i < hz.length; i++)
            for (int j = i + 1; j < hz.length; j++) combos.add(List.of(hz[i], hz[j]));

        ScenarioGenerator gen = new ScenarioGenerator();
        RepairSimulator sim = new RepairSimulator();
        RecoveryStrategy[] strategies = {new BaselineStrategy(), new CriticalityPerHourStrategy(), new IntegratedStrategy()};

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of("results_ablation.csv")))) {
            out.println("scenario,severity,crews,n,wd_base,wd_cph,wd_int,waste_base,waste_cph,waste_int");
            System.out.printf("%-32s %-6s %2s %4s | %8s %8s %8s | %6s %6s %6s%n",
                    "scenario", "sev", "cr", "n", "wdBase", "wdCPH", "wdInt", "wstB", "wstC", "wstI");
            for (List<HazardType> combo : combos) {
                String name = combo.stream().map(Enum::name).collect(Collectors.joining("+"));
                for (Severity sev : new Severity[]{Severity.MEDIUM, Severity.HIGH}) {
                    for (int crews = 1; crews <= 3; crews++) {
                        double[] wd = new double[3], ws = new double[3];
                        int n = 0;
                        for (long seed = 1; seed <= 100; seed++) {
                            HarbourGraph g = AppendixAHarbour.build();
                            List<Integer> faults = gen.injectFaults(g, new Scenario(combo, sev, 1.2, seed));
                            if (faults.size() < MIN_FAULTS) continue;
                            Map<Integer, Double> conf = new HashMap<>();
                            for (int id : faults) conf.put(id, 1.0);
                            n++;
                            for (int s = 0; s < 3; s++) {
                                List<RepairTask> t = sim.simulate(g, strategies[s], faults, conf, crews);
                                MetricsCalculator m = new MetricsCalculator(g);
                                wd[s] += m.weightedDowntime(t);
                                ws[s] += m.wastedRepairHours(t);
                            }
                        }
                        if (n == 0) continue;
                        System.out.printf("%-32s %-6s %2d %4d | %8.1f %8.1f %8.1f | %6.1f %6.1f %6.1f%n",
                                name, sev, crews, n, wd[0] / n, wd[1] / n, wd[2] / n, ws[0] / n, ws[1] / n, ws[2] / n);
                        out.printf("%s,%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                                name, sev, crews, n, wd[0] / n, wd[1] / n, wd[2] / n, ws[0] / n, ws[1] / n, ws[2] / n);
                    }
                }
            }
        }
    }
}