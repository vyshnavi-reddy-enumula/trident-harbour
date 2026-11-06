package com.trident.harbour.demo;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.service.DependencyAnalyzer;
import com.trident.harbour.sim.Scenario;
import com.trident.harbour.sim.ScenarioGenerator;
import com.trident.harbour.sim.Severity;

import java.util.ArrayList;
import java.util.List;

public final class FaultCensus {

    private static final int RUNS = 100;
    private static final double WEATHER = 1.2;

    public static void main(String[] args) {
        HazardType[] hz = HazardType.values();
        List<List<HazardType>> combos = new ArrayList<>();
        for (int i = 0; i < hz.length; i++) {
            combos.add(List.of(hz[i]));
        }
        for (int i = 0; i < hz.length; i++) {
            for (int j = i + 1; j < hz.length; j++) {
                combos.add(List.of(hz[i], hz[j]));
            }
        }

        ScenarioGenerator gen = new ScenarioGenerator();
        System.out.printf("%-30s %-7s %5s %5s %5s %8s %9s%n",
                "hazards", "sev", "minF", "avgF", "maxF", ">=4 (%)", "avgDown");

        for (List<HazardType> combo : combos) {
            for (Severity sev : Severity.values()) {
                int min = Integer.MAX_VALUE, max = 0, sum = 0, ok = 0, downSum = 0;
                for (long seed = 1; seed <= RUNS; seed++) {
                    HarbourGraph g = AppendixAHarbour.build();
                    List<Integer> faults =
                            gen.injectFaults(g, new Scenario(combo, sev, WEATHER, seed));
                    int n = faults.size();
                    min = Math.min(min, n);
                    max = Math.max(max, n);
                    sum += n;
                    if (n >= 4) ok++;
                    downSum += new DependencyAnalyzer(g).findCascade(faults).size();
                }
                System.out.printf("%-30s %-7s %5d %5.1f %5d %8d %9.1f%n",
                        combo, sev, min, sum / (double) RUNS, max, ok, downSum / (double) RUNS);
            }
        }
    }
}