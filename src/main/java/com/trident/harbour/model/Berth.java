package com.trident.harbour.model;

public class Berth extends Asset {

    public Berth(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.BERTH;
    }

    @Override
    public String getServiceProvided() {
        return "Vessel berthing";
    }

    @Override
    public double getFailureThreshold() {
        return 25.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.5;
            case POWER_OUTAGE -> 0.1;
            case EQUIPMENT_FAILURE -> 0.3;
        };
    }
}
