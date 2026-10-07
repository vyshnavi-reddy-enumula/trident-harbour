package com.trident.harbour.model;

/** Must match the values stored in the asset.type column of the database. */
public enum AssetType {
    POWER_SOURCE, SUBSTATION, PUMP, DRYDOCK, COMMS_TOWER,
    CONTROL_CENTRE, CRANE, BERTH, FUEL_JETTY, ACCESS_GATE
}
