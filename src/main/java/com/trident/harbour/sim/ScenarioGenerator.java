package com.trident.harbour.sim;

import com.trident.harbour.model.Asset;
import com.trident.harbour.model.AssetStatus;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ScenarioGenerator {

    // Assumed spread of damage around the nominal value (invented; sensitivity-test it).
    public static final double MIN_MULTIPLIER = 0.5;
    public static final double MAX_MULTIPLIER = 1.5;

    /**
     * Damages the harbour as the scenario says and returns the ids of assets that
     * now have their own fault (status FAILED), in ascending id order.
     * Pass a FRESH harbour for every run; this method changes asset health.
     */
    public List<Integer> injectFaults(HarbourGraph graph, Scenario scenario) {
        Random random = new Random(scenario.getSeed());
        List<Asset> assets = new ArrayList<>(graph.getAllAssets());
        assets.sort(Comparator.comparingInt(Asset::getId));

        for (HazardType hazard : scenario.getHazards()) {
            for (Asset asset : assets) {
                // always draw, even if the asset is barely vulnerable, so the sequence is stable
                double multiplier = MIN_MULTIPLIER
                        + random.nextDouble() * (MAX_MULTIPLIER - MIN_MULTIPLIER);
                double damage = scenario.getSeverity().getBaseDamage()
                        * asset.getVulnerability(hazard)
                        * scenario.getWeatherFactor()
                        * multiplier;
                asset.applyDamage(damage);
            }
        }

        List<Integer> failed = new ArrayList<>();
        for (Asset asset : assets) {
            if (asset.getStatus() == AssetStatus.FAILED) {
                failed.add(asset.getId());
            }
        }
        return failed;
    }
}