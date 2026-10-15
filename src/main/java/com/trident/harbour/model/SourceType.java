package com.trident.harbour.model;

public enum SourceType {
    SENSOR(0.9),
    CREW(0.7),
    WEATHER_FEED(0.5);

    private final double reliability;

    SourceType(double reliability) {
        this.reliability = reliability;
    }

    public double getReliability() {
        return reliability;
    }
}