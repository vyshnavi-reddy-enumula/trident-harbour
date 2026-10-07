package com.trident.harbour.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HarbourGraphTest {

    private HarbourGraph graph;

    // Substation is used as a stand-in; the graph only cares about ids.
    private Asset asset(int id) {
        return new Substation(id, "Asset " + id, 3, 4.0, 100.0);
    }

    @BeforeEach
    void setUp() {
        graph = new HarbourGraph();
    }

    // ---- adding assets ----

    @Test
    void addAsset_storesAsset() {
        graph.addAsset(asset(1));
        assertTrue(graph.hasAsset(1));
        assertEquals(1, graph.assetCount());
        assertEquals("Asset 1", graph.getAsset(1).getName());
    }

    @Test
    void addAsset_duplicateId_throws() {
        graph.addAsset(asset(1));
        assertThrows(DuplicateAssetException.class, () -> graph.addAsset(asset(1)));
    }

    @Test
    void addAsset_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> graph.addAsset(null));
    }

    // ---- adding dependencies ----

    @Test
    void addDependency_visibleInBothDirections() {
        graph.addAsset(asset(1)); // parent
        graph.addAsset(asset(2)); // child
        graph.addDependency(2, 1, DependencyType.REQUIRED);

        assertEquals(1, graph.getDependencies(2).size());
        assertEquals(1, graph.getDependents(1).size());
        assertEquals(1, graph.getDependencies(2).get(0).getDependsOnId());
        assertEquals(2, graph.getDependents(1).get(0).getAssetId());
        assertEquals(1, graph.dependencyCount());
    }

    @Test
    void addDependency_selfDependency_throws() {
        graph.addAsset(asset(1));
        assertThrows(InvalidDependencyException.class,
                () -> graph.addDependency(1, 1, DependencyType.REQUIRED));
    }

    @Test
    void addDependency_duplicate_throws() {
        graph.addAsset(asset(1));
        graph.addAsset(asset(2));
        graph.addDependency(2, 1, DependencyType.REQUIRED);
        assertThrows(InvalidDependencyException.class,
                () -> graph.addDependency(2, 1, DependencyType.BACKUP));
    }

    @Test
    void addDependency_missingAsset_throws() {
        graph.addAsset(asset(1));
        assertThrows(AssetNotFoundException.class,
                () -> graph.addDependency(1, 99, DependencyType.REQUIRED));
        assertThrows(AssetNotFoundException.class,
                () -> graph.addDependency(99, 1, DependencyType.REQUIRED));
    }

    @Test
    void addDependency_nullType_throws() {
        graph.addAsset(asset(1));
        graph.addAsset(asset(2));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addDependency(2, 1, null));
    }

    // ---- cycle detection ----

    @Test
    void addDependency_directCycle_throws() {
        graph.addAsset(asset(1));
        graph.addAsset(asset(2));
        graph.addDependency(2, 1, DependencyType.REQUIRED); // 2 depends on 1
        assertThrows(InvalidDependencyException.class,
                () -> graph.addDependency(1, 2, DependencyType.REQUIRED)); // 1 on 2 = loop
    }

    @Test
    void addDependency_indirectCycle_throws() {
        graph.addAsset(asset(1));
        graph.addAsset(asset(2));
        graph.addAsset(asset(3));
        graph.addDependency(2, 1, DependencyType.REQUIRED); // 2 -> 1
        graph.addDependency(3, 2, DependencyType.REQUIRED); // 3 -> 2
        assertThrows(InvalidDependencyException.class,
                () -> graph.addDependency(1, 3, DependencyType.REQUIRED)); // 1 -> 3 closes loop
    }

    @Test
    void addDependency_diamondShape_isAllowed() {
        // 4 depends on 2 and 3; both depend on 1. Shared parent is NOT a cycle.
        for (int i = 1; i <= 4; i++) graph.addAsset(asset(i));
        graph.addDependency(2, 1, DependencyType.REQUIRED);
        graph.addDependency(3, 1, DependencyType.REQUIRED);
        graph.addDependency(4, 2, DependencyType.REQUIRED);
        assertDoesNotThrow(() -> graph.addDependency(4, 3, DependencyType.REQUIRED));
        assertEquals(4, graph.dependencyCount());
    }
}