package com.trident.harbour.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityScorerTest {

    @Test
    void formulaComputesCorrectly() {
        assertEquals(1.0, PriorityScorer.priority(10, 0.5, 5.0), 0.001);
    }

    @Test
    void zeroUnlockedMeansZeroPriority() {
        assertEquals(0.0, PriorityScorer.priority(0, 1.0, 4.0), 0.001);
    }

    @Test
    void shorterRepairWinsWhenOtherTermsEqual() {
        assertTrue(PriorityScorer.priority(20, 0.9, 2.0) > PriorityScorer.priority(20, 0.9, 8.0));
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriorityScorer.priority(10, 1.5, 4.0));
        assertThrows(IllegalArgumentException.class, () -> PriorityScorer.priority(10, 0.5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> PriorityScorer.priority(-1, 0.5, 4.0));
    }
}