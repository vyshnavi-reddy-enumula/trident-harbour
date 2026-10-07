package com.trident.harbour.model;

public class ControlCentre extends Asset {

    public ControlCentre(int id, String name, int criticality, double repairHours, double healthScore) {
        super(id, name, criticality, repairHours, healthScore);
    }

    @Override
    public AssetType getType() {
        return AssetType.CONTROL_CENTRE;
    }

    @Override
    public String getServiceProvided() {
        return "Harbour command";
    }

    @Override
    public double getFailureThreshold() {
        return 40.0;
    }

    @Override
    public double getVulnerability(HazardType hazard) {
        return switch (hazard) {
            case FLOOD -> 0.3;
            case POWER_OUTAGE -> 0.2;
            case EQUIPMENT_FAILURE -> 0.3;
        };
    }
}
