package com.trident.harbour.model;

public class Drydock extends Asset {

    public Drydock(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.DRYDOCK;
    }

    @Override
    public String getServiceProvided() {
        return "Ship maintenance";
    }

    @Override
    public double getFailureThreshold() {
        return 30.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.5;
            case POWER_OUTAGE -> 0.2;
            case EQUIPMENT_FAILURE -> 0.4;
        };
    }
}
