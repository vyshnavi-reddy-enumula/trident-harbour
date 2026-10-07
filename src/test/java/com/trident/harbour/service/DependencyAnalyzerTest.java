package com.trident.harbour.service;

import com.trident.harbour.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyAnalyzerTest {

    private HarbourGraph graph;
    private DependencyAnalyzer analyzer;

    /*
     *  1 (grid) <-REQ- 2 (substation) <-REQ- 3 (pump) <-REQ- 4 (berth)
     *  1 <-BACKUP- 5
     *  2 <-REQ- 7 ;  6 depends REQUIRED on both 2 and 7   (diamond)
     */
    @BeforeEach
    void setUp() {
        graph = new HarbourGraph();
        for (int i = 1; i <= 7; i++) {
            graph.addAsset(new Substation(i, "Asset " + i, 3, 4.0, 100.0));
        }
        graph.addDependency(2, 1, DependencyType.REQUIRED);
        graph.addDependency(3, 2, DependencyType.REQUIRED);
        graph.addDependency(4, 3, DependencyType.REQUIRED);
        graph.addDependency(5, 1, DependencyType.BACKUP);
        graph.addDependency(7, 2, DependencyType.REQUIRED);
        graph.addDependency(6, 2, DependencyType.REQUIRED);
        graph.addDependency(6, 7, DependencyType.REQUIRED);
        analyzer = new DependencyAnalyzer(graph);
    }

    @Test
    void failRoot_cascadesThroughRequiredChainAndDiamond() {
        assertEquals(Set.of(3, 4, 6, 7), analyzer.findCascade(List.of(2)));
    }

    @Test
    void failLeaf_causesNoCascade() {
        assertTrue(analyzer.findCascade(List.of(4)).isEmpty());
    }

    @Test
    void failMiddle_doesNotAffectUpstream() {
        Set<Integer> result = analyzer.findCascade(List.of(3));
        assertEquals(Set.of(4), result);
        assertFalse(result.contains(2));
    }

    @Test
    void backupDependency_doesNotCascade() {
        Set<Integer> result = analyzer.findCascade(List.of(1));
        assertFalse(result.contains(5));
        assertEquals(Set.of(2, 3, 4, 6, 7), result);
    }

    @Test
    void multipleFailures_areCombined() {
        assertEquals(Set.of(4, 6), analyzer.findCascade(List.of(3, 7)));
    }

    @Test
    void unknownId_throws() {
        assertThrows(AssetNotFoundException.class,
                () -> analyzer.findCascade(List.of(99)));
    }
        @Test
    void criticalityUnlocked_singleFault_countsAssetAndAllDownstream() {
        // fail 1 -> blocked {1,2,3,4,6,7} = 6 assets x 3
        assertEquals(18, analyzer.criticalityUnlocked(1, List.of(1)));
    }

    @Test
    void criticalityUnlocked_fixDownstreamWhileRootDown_unlocksNothing() {
        // the wasted-repair case: pump 3 fixed, but grid 1 is still down
        assertEquals(0, analyzer.criticalityUnlocked(3, List.of(1, 3)));
    }

    @Test
    void criticalityUnlocked_fixRootWithDownstreamFaultStillPresent() {
        // 1,2,6,7 come back; 3 and 4 stay down because 3 is still faulty
        assertEquals(12, analyzer.criticalityUnlocked(1, List.of(1, 3)));
    }

    @Test
    void criticalityUnlocked_assetNotFailed_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.criticalityUnlocked(2, List.of(1)));
    }
}