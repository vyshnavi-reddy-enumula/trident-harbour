package com.trident.harbour.sim;

import com.trident.harbour.model.HazardType;

import java.util.List;

public final class Scenario {

    private final List<HazardType> hazards;
    private final Severity severity;
    private final double weatherFactor;   // 1.0 = calm; higher raises damage
    private final long seed;

    public Scenario(List<HazardType> hazards, Severity severity, double weatherFactor, long seed) {
        if (hazards == null || hazards.isEmpty()) {
            throw new IllegalArgumentException("A scenario needs at least one hazard");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null");
        }
        if (!(weatherFactor >= 1.0)) {
            throw new IllegalArgumentException("weatherFactor must be >= 1.0 but was " + weatherFactor);
        }
        this.hazards = List.copyOf(hazards);
        this.severity = severity;
        this.weatherFactor = weatherFactor;
        this.seed = seed;
    }

    public List<HazardType> getHazards() { return hazards; }
    public Severity getSeverity() { return severity; }
    public double getWeatherFactor() { return weatherFactor; }
    public long getSeed() { return seed; }
}