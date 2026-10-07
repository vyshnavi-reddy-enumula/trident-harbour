package com.trident.harbour.demo;

import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.service.DependencyAnalyzer;

import java.util.List;
import java.util.Set;

public class DoubleFaultDemo {

    public static void main(String[] args) {
        HarbourGraph g = AppendixAHarbour.build();
        DependencyAnalyzer analyzer = new DependencyAnalyzer(g);

        List<Integer> failed = List.of(AppendixAHarbour.SUBSTATION_A, AppendixAHarbour.PUMP_1);
        for (int id : failed) {
            g.getAsset(id).setHealthScore(0);
            System.out.println("FAULT: " + g.getAsset(id));
        }

        Set<Integer> cascade = analyzer.findCascade(failed);
        System.out.println("\nA conventional dashboard raises "
                + (failed.size() + cascade.size()) + " separate alerts.");
        System.out.println("Only " + failed.size() + " are real faults; "
                + cascade.size() + " are knock-on effects.");

        System.out.println("\nIf a crew fixes this FIRST, criticality restored:");
        for (int id : failed) {
            System.out.println("  - " + g.getAsset(id).getName() + ": "
                    + analyzer.criticalityUnlocked(id, failed));
        }
    }
}