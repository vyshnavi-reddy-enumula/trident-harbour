package com.trident.harbour.service;

public final class PriorityScorer {

    private PriorityScorer() {}

    /**
     * Priority = (criticalityUnlocked * confidence) / repairHours.
     * Higher means repair sooner.
     */
    public static double priority(int criticalityUnlocked, double confidence, double repairHours) {
        if (criticalityUnlocked < 0) {
            throw new IllegalArgumentException("criticalityUnlocked must be >= 0 but was " + criticalityUnlocked);
        }
        if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be 0..1 but was " + confidence);
        }
        if (Double.isNaN(repairHours) || repairHours <= 0.0) {
            throw new IllegalArgumentException("repairHours must be positive but was " + repairHours);
        }
        return (criticalityUnlocked * confidence) / repairHours;
    }
}