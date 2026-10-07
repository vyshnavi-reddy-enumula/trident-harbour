package com.trident.harbour.model;

public class Crane extends Asset {

    public Crane(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.CRANE;
    }

    @Override
    public String getServiceProvided() {
        return "Loading and unloading";
    }

    @Override
    public double getFailureThreshold() {
        return 30.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.3;
            case POWER_OUTAGE -> 0.2;
            case EQUIPMENT_FAILURE -> 0.6;
        };
    }
}
