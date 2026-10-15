package com.trident.harbour.model;

public final class RepairTask {
    private final int assetId;
    private final int crewId;
    private final double startTime;
    private final double endTime;
    private final boolean wasted;

    public RepairTask(int assetId, int crewId, double startTime, double endTime, boolean wasted) {
        this.assetId = assetId;
        this.crewId = crewId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.wasted = wasted;
    }

    public int getAssetId() { return assetId; }
    public int getCrewId() { return crewId; }
    public double getStartTime() { return startTime; }
    public double getEndTime() { return endTime; }
    public double getHours() { return endTime - startTime; }

    /** True if an upstream fault was still unrepaired when this repair finished. */
    public boolean isWasted() { return wasted; }
}