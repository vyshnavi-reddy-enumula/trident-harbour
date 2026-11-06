package com.trident.harbour.demo;

import com.trident.harbour.model.AssetType;
import com.trident.harbour.model.HarbourGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppendixAHarbourTest {

    @Test
    void usesRealSubclassesAndTheRepairHoursTable() {
        HarbourGraph g = AppendixAHarbour.build();
        assertEquals(AssetType.PUMP, g.getAsset(AppendixAHarbour.PUMP_1).getType());
        assertEquals(AssetType.CRANE, g.getAsset(10).getType());
        assertEquals(AssetType.ACCESS_GATE, g.getAsset(AppendixAHarbour.ACCESS_ROAD_GATE).getType());
        assertEquals(8.0, g.getAsset(1).getRepairHours(), 1e-9);
        assertEquals(4.0, g.getAsset(AppendixAHarbour.SUBSTATION_A).getRepairHours(), 1e-9);
    }
}