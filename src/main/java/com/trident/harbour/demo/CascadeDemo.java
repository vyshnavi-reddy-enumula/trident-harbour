package com.trident.harbour.demo;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.DependencyType;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.Substation;
import com.trident.harbour.service.DependencyAnalyzer;

import java.util.List;
import java.util.Set;

public class CascadeDemo {

    // Placeholder: every asset is a Substation for now, so only names and criticality matter.
    // Swap in your real subclasses (Pump, Crane, Berth...) when you wire up the DB loader.
    private static Asset asset(int id, String name, int criticality) {
        return new Substation(id, name, criticality, 4.0, 100.0);
    }

    public static void main(String[] args) {
        HarbourGraph g = new HarbourGraph();

        g.addAsset(asset(1, "Main Grid Feed", 5));
        g.addAsset(asset(2, "Backup Generator", 3));
        g.addAsset(asset(3, "Substation A", 5));
        g.addAsset(asset(4, "Drainage Pump Station 1", 4));
        g.addAsset(asset(5, "Drainage Pump Station 2", 3));
        g.addAsset(asset(6, "Drydock Dewatering Pump", 4));
        g.addAsset(asset(7, "Drydock", 4));
        g.addAsset(asset(8, "Comms Tower", 5));
        g.addAsset(asset(9, "Control Centre", 5));
        g.addAsset(asset(10, "Crane 1", 3));
        g.addAsset(asset(11, "Crane 2", 3));
        g.addAsset(asset(12, "Berth 1", 4));
        g.addAsset(asset(13, "Berth 2", 4));
        g.addAsset(asset(14, "Fuel Jetty", 4));
        g.addAsset(asset(15, "Access Road and Gate", 3));

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

        System.out.println("Harbour loaded: " + g.assetCount() + " assets, "
                + g.dependencyCount() + " dependencies");

        // Inject the fault
        g.getAsset(3).setHealthScore(0);
        System.out.println("\nFAULT: " + g.getAsset(3));

        DependencyAnalyzer analyzer = new DependencyAnalyzer(g);
        Set<Integer> cascade = analyzer.findCascade(List.of(3));

        System.out.println("\nCascade: " + cascade.size() + " assets knocked out");
        int stuckCriticality = 0;
        for (int id : cascade) {
            Asset a = g.getAsset(id);
            System.out.println("  - " + a.getName() + " (criticality " + a.getCriticality() + ")");
            stuckCriticality += a.getCriticality();
        }
        System.out.println("\nCriticality stuck behind Substation A: " + stuckCriticality);
    }
}