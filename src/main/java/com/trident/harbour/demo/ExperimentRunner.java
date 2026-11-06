package com.trident.harbour.demo;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.RepairTask;
import com.trident.harbour.service.BaselineStrategy;
import com.trident.harbour.service.IntegratedStrategy;
import com.trident.harbour.service.MetricsCalculator;
import com.trident.harbour.service.RecoveryStrategy;
import com.trident.harbour.service.RepairSimulator;
import com.trident.harbour.sim.Scenario;
import com.trident.harbour.sim.ScenarioGenerator;
import com.trident.harbour.sim.Severity;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Paired experiment: for every (scenario type, severity, crew count, seed) both
 * strategies see the SAME faults. Rules fixed before running, do not tune after:
 * severity MEDIUM/HIGH only, weather 1.2, seeds 1..100, skip seeds with < 2 faults.
 */
public final class ExperimentRunner {

    private static final int SEEDS = 100;
    private static final double WEATHER = 1.2;
    private static final int MIN_FAULTS = 2;
    private static final int[] CREWS = {1, 2, 3};
    private static final Severity[] SEVERITIES = {Severity.MEDIUM, Severity.HIGH};
    private static final double EPS = 1e-9;

    private record Result(double downtime, double critical, double wasted) {}

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        HazardType[] hz = HazardType.values();
        List<List<HazardType>> combos = new ArrayList<>();
        for (HazardType h : hz) {
            combos.add(List.of(h));
        }
        for (int i = 0; i < hz.length; i++) {
            for (int j = i + 1; j < hz.length; j++) {
                combos.add(List.of(hz[i], hz[j]));
            }
        }

        ScenarioGenerator gen = new ScenarioGenerator();
        RepairSimulator sim = new RepairSimulator();
        RecoveryStrategy baseline = new BaselineStrategy();
        RecoveryStrategy integrated = new IntegratedStrategy();

        try (PrintWriter runs = new PrintWriter(Files.newBufferedWriter(Path.of("results_runs.csv")));
             PrintWriter summary = new PrintWriter(Files.newBufferedWriter(Path.of("results_summary.csv")))) {

            runs.println("scenario,severity,crews,seed,faults,strategy,weighted_downtime,time_to_critical,wasted_hours");
            summary.println("scenario,severity,crews,n,skipped,"
                    + "wd_base_mean,wd_base_sd,wd_int_mean,wd_int_sd,wd_impr_pct,wd_wins,wd_ties,wd_losses,"
                    + "ttc_base_mean,ttc_base_sd,ttc_int_mean,ttc_int_sd,ttc_impr_pct,"
                    + "waste_base_mean,waste_base_sd,waste_int_mean,waste_int_sd,waste_impr_pct");

            System.out.printf("%-32s %-6s %2s %4s %4s | %8s %8s %6s | %5s %5s %6s | %6s %6s %6s | W/T/L(wd)%n",
                    "scenario", "sev", "cr", "n", "skip",
                    "wdBase", "wdInt", "impr%", "ttcB", "ttcI", "impr%", "wstB", "wstI", "impr%");

            for (List<HazardType> combo : combos) {
                String name = combo.stream().map(Enum::name).collect(Collectors.joining("+"));
                for (Severity sev : SEVERITIES) {
                    for (int crews : CREWS) {
                        List<Double> bwd = new ArrayList<>(), iwd = new ArrayList<>();
                        List<Double> bttc = new ArrayList<>(), ittc = new ArrayList<>();
                        List<Double> bw = new ArrayList<>(), iw = new ArrayList<>();
                        int skipped = 0, wins = 0, ties = 0, losses = 0;

                        for (long seed = 1; seed <= SEEDS; seed++) {
                            HarbourGraph g = AppendixAHarbour.build();
                            List<Integer> faults = gen.injectFaults(g, new Scenario(combo, sev, WEATHER, seed));
                            if (faults.size() < MIN_FAULTS) {
                                skipped++;
                                continue;
                            }
                            // phase 1: every real fault is fully confirmed
                            java.util.Map<Integer, Double> conf = new java.util.HashMap<>();
                            for (int id : faults) {
                                conf.put(id, 1.0);
                            }

                            Result b = evaluate(sim, g, baseline, faults, conf, crews);
                            Result i = evaluate(sim, g, integrated, faults, conf, crews);

                            bwd.add(b.downtime()); iwd.add(i.downtime());
                            bttc.add(b.critical()); ittc.add(i.critical());
                            bw.add(b.wasted()); iw.add(i.wasted());

                            if (i.downtime() < b.downtime() - EPS) wins++;
                            else if (i.downtime() > b.downtime() + EPS) losses++;
                            else ties++;

                            runs.printf("%s,%s,%d,%d,%d,BASELINE,%.2f,%.2f,%.2f%n",
                                    name, sev, crews, seed, faults.size(), b.downtime(), b.critical(), b.wasted());
                            runs.printf("%s,%s,%d,%d,%d,INTEGRATED,%.2f,%.2f,%.2f%n",
                                    name, sev, crews, seed, faults.size(), i.downtime(), i.critical(), i.wasted());
                        }

                        int n = bwd.size();
                        if (n == 0) {
                            System.out.printf("%-32s %-6s %2d %4d %4d | (no usable seeds)%n",
                                    name, sev, crews, 0, skipped);
                            continue;
                        }
                        double bwdM = mean(bwd), iwdM = mean(iwd);
                        double bttcM = mean(bttc), ittcM = mean(ittc);
                        double bwM = mean(bw), iwM = mean(iw);

                        System.out.printf("%-32s %-6s %2d %4d %4d | %8.1f %8.1f %6s | %5.1f %5.1f %6s | %6.1f %6.1f %6s | %d/%d/%d%n",
                                name, sev, crews, n, skipped,
                                bwdM, iwdM, pct(bwdM, iwdM),
                                bttcM, ittcM, pct(bttcM, ittcM),
                                bwM, iwM, pct(bwM, iwM),
                                wins, ties, losses);

                        summary.printf("%s,%s,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%s,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%s,%.2f,%.2f,%.2f,%.2f,%s%n",
                                name, sev, crews, n, skipped,
                                bwdM, sd(bwd), iwdM, sd(iwd), pct(bwdM, iwdM), wins, ties, losses,
                                bttcM, sd(bttc), ittcM, sd(ittc), pct(bttcM, ittcM),
                                bwM, sd(bw), iwM, sd(iw), pct(bwM, iwM));
                    }
                }
            }
        }
        System.out.println("\nWrote results_runs.csv and results_summary.csv");
    }

    private static Result evaluate(RepairSimulator sim, HarbourGraph g, RecoveryStrategy strategy,
                                   List<Integer> faults, java.util.Map<Integer, Double> conf, int crews) {
        List<RepairTask> tasks = sim.simulate(g, strategy, faults, conf, crews);
        MetricsCalculator m = new MetricsCalculator(g);
        return new Result(m.weightedDowntime(tasks), m.timeToRestoreCritical(tasks), m.wastedRepairHours(tasks));
    }

    private static double mean(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /** Sample standard deviation. */
    private static double sd(List<Double> xs) {
        if (xs.size() < 2) {
            return 0.0;
        }
        double m = mean(xs);
        double ss = 0.0;
        for (double x : xs) {
            ss += (x - m) * (x - m);
        }
        return Math.sqrt(ss / (xs.size() - 1));
    }

    /** Percentage improvement of the integrated mean over the baseline mean; "-" if baseline is 0. */
    private static String pct(double base, double integ) {
        if (base < EPS) {
            return "-";
        }
        return String.format("%.1f", (base - integ) / base * 100.0);
    }
}