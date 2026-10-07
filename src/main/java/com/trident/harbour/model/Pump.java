package com.trident.harbour.model;

public class Pump extends Asset {

    public Pump(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.PUMP;
    }

    @Override
    public String getServiceProvided() {
        return "Flood or water control";
    }

    @Override
    public double getFailureThreshold() {
        return 35.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.8;
            case POWER_OUTAGE -> 0.2;
            case EQUIPMENT_FAILURE -> 0.5;
        };
    }
}
