package com.trident.harbour.sim;

public enum Severity {
    // Assumed base damage per severity level (invented; sensitivity-test it).
    LOW(30.0), MEDIUM(55.0), HIGH(80.0);

    private final double baseDamage;

    Severity(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public double getBaseDamage() {
        return baseDamage;
    }
}