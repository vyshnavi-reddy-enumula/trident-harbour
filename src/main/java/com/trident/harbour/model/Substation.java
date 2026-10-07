package com.trident.harbour.model;

public class Substation extends Asset {

    public Substation(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.SUBSTATION;
    }

    @Override
    public String getServiceProvided() {
        return "Power distribution";
    }

    @Override
    public double getFailureThreshold() {
        return 40.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.6;
            case POWER_OUTAGE -> 0.9;
            case EQUIPMENT_FAILURE -> 0.5;
        };
    }
}
