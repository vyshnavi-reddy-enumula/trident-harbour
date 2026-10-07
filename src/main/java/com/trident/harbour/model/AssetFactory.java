package com.trident.harbour.model;

import com.trident.harbour.exception.UnknownAssetTypeException;

/**
 * Builds the right Asset subclass from raw database values.
 * The DAO layer calls this so no other code needs to know about concrete subclasses.
 */
public final class AssetFactory {

    private AssetFactory() { }

    public static Asset create(int id, String name, String type,
                               int criticality, double repairHours, double healthScore) {
        AssetType assetType;
        try {
            assetType = AssetType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UnknownAssetTypeException(type);
        }
        return switch (assetType) {
            case POWER_SOURCE   -> new PowerSource(id, name, criticality, repairHours, healthScore);
            case SUBSTATION     -> new Substation(id, name, criticality, repairHours, healthScore);
            case PUMP           -> new Pump(id, name, criticality, repairHours, healthScore);
            case DRYDOCK        -> new Drydock(id, name, criticality, repairHours, healthScore);
            case COMMS_TOWER    -> new CommsTower(id, name, criticality, repairHours, healthScore);
            case CONTROL_CENTRE -> new ControlCentre(id, name, criticality, repairHours, healthScore);
            case CRANE          -> new Crane(id, name, criticality, repairHours, healthScore);
            case BERTH          -> new Berth(id, name, criticality, repairHours, healthScore);
            case FUEL_JETTY     -> new FuelJetty(id, name, criticality, repairHours, healthScore);
            case ACCESS_GATE    -> new AccessGate(id, name, criticality, repairHours, healthScore);
        };
    }
}
