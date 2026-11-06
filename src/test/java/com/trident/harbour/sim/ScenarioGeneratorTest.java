package com.trident.harbour.sim;

import com.trident.harbour.demo.AppendixAHarbour;
import com.trident.harbour.model.AssetStatus;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.model.HazardType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioGeneratorTest {

    private final ScenarioGenerator generator = new ScenarioGenerator();

    private List<Integer> run(Scenario scenario) {
        HarbourGraph freshHarbour = AppendixAHarbour.build();
        return generator.injectFaults(freshHarbour, scenario);
    }

    @Test
    void sameSeedGivesSameFaults() {
        Scenario s = new Scenario(List.of(HazardType.POWER_OUTAGE), Severity.HIGH, 1.2, 42L);
        assertEquals(run(s), run(s));
    }

    @Test
    void faultsAreSortedAndReallyFailed() {
        Scenario s = new Scenario(
                List.of(HazardType.FLOOD, HazardType.POWER_OUTAGE), Severity.HIGH, 1.3, 7L);
        HarbourGraph g = AppendixAHarbour.build();
        List<Integer> failed = generator.injectFaults(g, s);

        for (int i = 1; i < failed.size(); i++) {
            assertTrue(failed.get(i - 1) < failed.get(i));
        }
        for (int id : failed) {
            assertEquals(AssetStatus.FAILED, g.getAsset(id).getStatus());
        }
    }

    @Test
    void rejectsScenarioWithNoHazard() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scenario(List.of(), Severity.LOW, 1.0, 1L));
    }
}