package com.trident.harbour.demo;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.Dependency;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.RepairTask;
import com.trident.harbour.service.BaselineStrategy;
import com.trident.harbour.service.DependencyAnalyzer;
import com.trident.harbour.service.IntegratedStrategy;
import com.trident.harbour.service.MetricsCalculator;
import com.trident.harbour.service.RecoveryStrategy;
import com.trident.harbour.service.RepairSimulator;
import com.trident.harbour.sim.Scenario;
import com.trident.harbour.sim.ScenarioGenerator;
import com.trident.harbour.sim.Severity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Writes dashboard_data.js for the HTML dashboard. Rule (fixed, no cherry-picking):
 * the first 3 usable seeds (>= 2 faults) of every scenario/severity, at 1, 2 and 3 crews,
 * plus ONE hand-made worked example (Substation A + Pump 1, 1 crew).
 */
public final class DashboardExporter {

    private static final int SEEDS_PER_CONFIG = 3;
    private static final double WEATHER = 1.2;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        ScenarioGenerator gen = new ScenarioGenerator();
        List<String> runs = new ArrayList<>();

        // hand-made worked example: pump is MORE damaged than the substation, so the baseline goes to the pump
        HarbourGraph demo = AppendixAHarbour.build();
        demo.getAsset(AppendixAHarbour.PUMP_1).applyDamage(95.0);
        demo.getAsset(AppendixAHarbour.SUBSTATION_A).applyDamage(80.0);
        runs.add(run("WORKED EXAMPLE: Substation A + Pump 1", "WORKED_EXAMPLE", "-", 0, 1,
                demo, List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1)));

        HazardType[] hz = HazardType.values();
        List<List<HazardType>> combos = new ArrayList<>();
        for (HazardType h : hz) combos.add(List.of(h));
        for (int i = 0; i < hz.length; i++)
            for (int j = i + 1; j < hz.length; j++) combos.add(List.of(hz[i], hz[j]));

        for (List<HazardType> combo : combos) {
            String name = combo.stream().map(Enum::name).collect(Collectors.joining("+"));
            for (Severity sev : new Severity[]{Severity.MEDIUM, Severity.HIGH}) {
                int found = 0;
                for (long seed = 1; seed <= 100 && found < SEEDS_PER_CONFIG; seed++) {
                    HarbourGraph g = AppendixAHarbour.build();
                    List<Integer> faults = gen.injectFaults(g, new Scenario(combo, sev, WEATHER, seed));
                    if (faults.size() < 2) continue;
                    found++;
                    for (int crews = 1; crews <= 3; crews++) {
                        runs.add(run(name + " " + sev + " seed " + seed, name, sev.name(), seed, crews, g, faults));
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder("window.DASH = {\n\"harbour\": ");
        sb.append(harbour(AppendixAHarbour.build())).append(",\n\"runs\": [\n");
        sb.append(String.join(",\n", runs)).append("\n]\n};\n");
        Files.writeString(Path.of("dashboard_data.js"), sb.toString());
        System.out.println("Wrote dashboard_data.js: " + runs.size() + " runs, " + sb.length() / 1024 + " KB");
    }

    private static String harbour(HarbourGraph g) {
        List<Asset> assets = new ArrayList<>(g.getAllAssets());
        assets.sort(Comparator.comparingInt(Asset::getId));
        List<String> nodes = new ArrayList<>(), edges = new ArrayList<>();
        for (Asset a : assets) {
            nodes.add(String.format("{\"id\":%d,\"name\":%s,\"crit\":%d,\"hours\":%.1f}",
                    a.getId(), q(a.getName()), a.getCriticality(), a.getRepairHours()));
            for (Dependency d : g.getDependencies(a.getId())) {
                edges.add(String.format("{\"from\":%d,\"to\":%d,\"type\":\"%s\"}",
                        d.getAssetId(), d.getDependsOnId(), d.getType().name()));
            }
        }
        return "{\"assets\":[" + String.join(",", nodes) + "],\"edges\":[" + String.join(",", edges) + "]}";
    }

    private static String run(String label, String scenario, String sev, long seed, int crews,
                              HarbourGraph g, List<Integer> faults) {
        Map<Integer, Double> conf = new HashMap<>();
        for (int id : faults) conf.put(id, 1.0);
        RepairSimulator sim = new RepairSimulator();
        return "{\"label\":" + q(label) + ",\"scenario\":" + q(scenario) + ",\"severity\":" + q(sev)
                + ",\"seed\":" + seed + ",\"crews\":" + crews + ",\"faults\":" + faults
                + ",\"strategies\":{\"BASELINE\":" + strategy(sim, g, new BaselineStrategy(), faults, conf, crews)
                + ",\"INTEGRATED\":" + strategy(sim, g, new IntegratedStrategy(), faults, conf, crews) + "}}";
    }

    static String strategy(RepairSimulator sim, HarbourGraph g, RecoveryStrategy s,
                                   List<Integer> faults, Map<Integer, Double> conf, int crews) {
        List<RepairTask> tasks = sim.simulate(g, s, faults, conf, crews);
        MetricsCalculator m = new MetricsCalculator(g);
        DependencyAnalyzer an = new DependencyAnalyzer(g);

        List<String> taskJson = new ArrayList<>();
        TreeSet<Double> times = new TreeSet<>();
        times.add(0.0);
        for (RepairTask t : tasks) {
            times.add(t.getEndTime());
            taskJson.add(String.format("{\"asset\":%d,\"crew\":%d,\"start\":%.2f,\"end\":%.2f,\"wasted\":%b}",
                    t.getAssetId(), t.getCrewId(), t.getStartTime(), t.getEndTime(), t.isWasted()));
        }
        List<String> stateJson = new ArrayList<>();
        for (double time : times) {
            List<Integer> unrepaired = new ArrayList<>();
            for (RepairTask t : tasks) if (t.getEndTime() > time) unrepaired.add(t.getAssetId());
            TreeSet<Integer> down = new TreeSet<>(unrepaired);
            down.addAll(an.findCascade(unrepaired));
            stateJson.add(String.format("{\"t\":%.2f,\"down\":%s}", time, down));
        }
        return String.format("{\"tasks\":[%s],\"states\":[%s],\"metrics\":{\"wd\":%.2f,\"ttc\":%.2f,\"wasted\":%.2f}}",
                String.join(",", taskJson), String.join(",", stateJson),
                m.weightedDowntime(tasks), m.timeToRestoreCritical(tasks), m.wastedRepairHours(tasks));
    }

    private static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}