package com.trident.harbour.model;

import com.trident.harbour.exception.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssetTest {

    private Asset substation() {
        return new Substation(3, "Substation A", 5, 6.0, 100.0);
    }

    @Test
    void newHealthyAssetIsOperational() {
        Asset a = substation();
        assertEquals(AssetStatus.OPERATIONAL, a.getStatus());
        assertTrue(a.isOperational());
    }

    @Test
    void damageBelowFailureThresholdFailsTheAsset() {
        Asset a = substation();            // failure threshold 40
        a.applyDamage(55);                 // health 45 -> degraded, not failed
        assertEquals(AssetStatus.DEGRADED, a.getStatus());
        a.applyDamage(10);                 // health 35 -> failed
        assertEquals(AssetStatus.FAILED, a.getStatus());
        assertFalse(a.isOperational());
    }

    @Test
    void healthNeverGoesBelowZero() {
        Asset a = substation();
        a.applyDamage(500);
        assertEquals(0.0, a.getHealthScore());
    }

    @Test
    void invalidHealthIsRejected() {
        assertThrows(InvalidHealthException.class, () -> new Pump(1, "P1", 4, 3, 101));
        assertThrows(InvalidHealthException.class, () -> substation().setHealthScore(-1));
    }

    @Test
    void invalidConstructorDataIsRejected() {
        assertThrows(InvalidAssetException.class, () -> new Crane(1, "C1", 6, 2, 100));
        assertThrows(InvalidAssetException.class, () -> new Crane(1, " ", 3, 2, 100));
        assertThrows(InvalidAssetException.class, () -> new Crane(1, "C1", 3, 0, 100));
    }

    @Test
    void repairLifecycleRestoresFullHealth() {
        Asset a = substation();
        a.applyDamage(80);
        a.startRepair();
        assertEquals(AssetStatus.UNDER_REPAIR, a.getStatus());
        a.completeRepair();
        assertEquals(100.0, a.getHealthScore());
        assertEquals(AssetStatus.OPERATIONAL, a.getStatus());
    }

    @Test
    void illegalRepairTransitionsThrow() {
        Asset a = substation();
        assertThrows(IllegalAssetStateException.class, a::startRepair);     // healthy
        assertThrows(IllegalAssetStateException.class, a::completeRepair);  // never started
        a.applyDamage(50);
        a.startRepair();
        assertThrows(IllegalAssetStateException.class, a::startRepair);     // already started
    }

    @Test
    void runtimePolymorphismThroughAssetReference() {
        List<Asset> assets = List.of(
                new Pump(4, "Drainage Pump 1", 4, 3, 100),
                new Substation(3, "Substation A", 5, 6, 100),
                new Berth(12, "Berth 1", 4, 4, 100));
        // pumps are far more flood-vulnerable than berths in our assumptions
        assertTrue(assets.get(0).getVulnerability(HazardType.FLOOD)
                 > assets.get(2).getVulnerability(HazardType.FLOOD));
        // same health, different thresholds -> different status
        assets.forEach(a -> a.setHealthScore(38));
        assertEquals(AssetStatus.FAILED, assets.get(1).getStatus());       // substation threshold 40
        assertEquals(AssetStatus.DEGRADED, assets.get(2).getStatus());     // berth threshold 25
    }

    @Test
    void factoryBuildsCorrectSubclassFromDbString() {
        Asset a = AssetFactory.create(7, "Crane 1", "crane", 3, 5.0, 100);
        assertInstanceOf(Crane.class, a);
        assertEquals(AssetType.CRANE, a.getType());
    }

    @Test
    void factoryRejectsUnknownType() {
        assertThrows(UnknownAssetTypeException.class,
                () -> AssetFactory.create(1, "X", "SPACESHIP", 3, 1, 100));
    }

    @Test
    void equalityIsById() {
        assertEquals(new Pump(1, "A", 3, 2, 100), new Pump(1, "B", 4, 9, 50));
    }
}
