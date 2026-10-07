package com.trident.harbour.demo;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.service.DependencyAnalyzer;

import java.util.List;
import java.util.Set;

public class CascadeDemo {

    public static void main(String[] args) {
        HarbourGraph g = AppendixAHarbour.build();

        System.out.println("Harbour loaded: " + g.assetCount() + " assets, "
                + g.dependencyCount() + " dependencies");

        g.getAsset(AppendixAHarbour.SUBSTATION_A).setHealthScore(0);
        System.out.println("\nFAULT: " + g.getAsset(AppendixAHarbour.SUBSTATION_A));

        DependencyAnalyzer analyzer = new DependencyAnalyzer(g);
        Set<Integer> cascade = analyzer.findCascade(List.of(AppendixAHarbour.SUBSTATION_A));

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