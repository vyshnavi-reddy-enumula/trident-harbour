package com.trident.harbour.demo;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.AssetFactory;
import com.trident.harbour.model.AssetType;
import com.trident.harbour.model.DependencyType;
import com.trident.harbour.model.HarbourGraph;

import java.util.Map;

public final class AppendixAHarbour {

    public static final int SUBSTATION_A = 3;
    public static final int PUMP_1 = 4;
    public static final int ACCESS_ROAD_GATE = 15;

    /**
     * Assumed repair hours (invented, justified by equipment size and complexity).
     * Substation A and Pump 1 stay at 4.0: the hand-checked tests depend on it.
     * Fixed before any experiment is run. Vary via build(Map) for sensitivity analysis.
     */
    public static final Map<Integer, Double> DEFAULT_REPAIR_HOURS = Map.ofEntries(
            Map.entry(1, 8.0),
            Map.entry(2, 3.0),
            Map.entry(3, 4.0),
            Map.entry(4, 4.0),
            Map.entry(5, 3.0),
            Map.entry(6, 5.0),
            Map.entry(7, 6.0),
            Map.entry(8, 5.0),
            Map.entry(9, 6.0),
            Map.entry(10, 6.0),
            Map.entry(11, 6.0),
            Map.entry(12, 3.0),
            Map.entry(13, 3.0),
            Map.entry(14, 5.0),
            Map.entry(15, 2.0));

    private AppendixAHarbour() {}

    public static HarbourGraph build() {
        return build(DEFAULT_REPAIR_HOURS);
    }

    /** Same harbour with different repair hours; ids missing from the map use the default. */
    public static HarbourGraph build(Map<Integer, Double> repairHours) {
        HarbourGraph g = new HarbourGraph();

        g.addAsset(asset(1, "Main Grid Feed", AssetType.POWER_SOURCE, 5, repairHours));
        g.addAsset(asset(2, "Backup Generator", AssetType.POWER_SOURCE, 3, repairHours));
        g.addAsset(asset(3, "Substation A", AssetType.SUBSTATION, 5, repairHours));
        g.addAsset(asset(4, "Drainage Pump Station 1", AssetType.PUMP, 4, repairHours));
        g.addAsset(asset(5, "Drainage Pump Station 2", AssetType.PUMP, 3, repairHours));
        g.addAsset(asset(6, "Drydock Dewatering Pump", AssetType.PUMP, 4, repairHours));
        g.addAsset(asset(7, "Drydock", AssetType.DRYDOCK, 4, repairHours));
        g.addAsset(asset(8, "Comms Tower", AssetType.COMMS_TOWER, 5, repairHours));
        g.addAsset(asset(9, "Control Centre", AssetType.CONTROL_CENTRE, 5, repairHours));
        g.addAsset(asset(10, "Crane 1", AssetType.CRANE, 3, repairHours));
        g.addAsset(asset(11, "Crane 2", AssetType.CRANE, 3, repairHours));
        g.addAsset(asset(12, "Berth 1", AssetType.BERTH, 4, repairHours));
        g.addAsset(asset(13, "Berth 2", AssetType.BERTH, 4, repairHours));
        g.addAsset(asset(14, "Fuel Jetty", AssetType.FUEL_JETTY, 4, repairHours));
        g.addAsset(asset(15, "Access Road and Gate", AssetType.ACCESS_GATE, 3, repairHours));

        g.addDependency(3, 1, DependencyType.REQUIRED);
        g.addDependency(3, 2, DependencyType.BACKUP);
        g.addDependency(4, 3, DependencyType.REQUIRED);
        g.addDependency(5, 3, DependencyType.REQUIRED);
        g.addDependency(6, 3, DependencyType.REQUIRED);
        g.addDependency(7, 6, DependencyType.REQUIRED);
        g.addDependency(8, 3, DependencyType.REQUIRED);
        g.addDependency(9, 3, DependencyType.REQUIRED);
        g.addDependency(9, 8, DependencyType.REQUIRED);
        g.addDependency(10, 3, DependencyType.REQUIRED);
        g.addDependency(11, 3, DependencyType.REQUIRED);
        g.addDependency(12, 10, DependencyType.REQUIRED);
        g.addDependency(12, 4, DependencyType.REQUIRED);
        g.addDependency(13, 11, DependencyType.REQUIRED);
        g.addDependency(13, 5, DependencyType.REQUIRED);
        g.addDependency(14, 3, DependencyType.REQUIRED);
        g.addDependency(14, 13, DependencyType.REQUIRED);
        g.addDependency(15, 4, DependencyType.REQUIRED);
        return g;
    }

    private static Asset asset(int id, String name, AssetType type, int criticality,
                               Map<Integer, Double> repairHours) {
        double hours = repairHours.getOrDefault(id, DEFAULT_REPAIR_HOURS.get(id));
        return AssetFactory.create(id, name, type.name(), criticality, hours, 100.0);
    }
}