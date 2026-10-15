package com.trident.harbour.model;

import java.util.Objects;

public final class EventReport {
    private final int assetId;
    private final HazardType hazardType;
    private final SourceType sourceType;
    private final int reportedSeverity;
    private final long timestamp;

    public EventReport(int assetId, HazardType hazardType, SourceType sourceType,
                       int reportedSeverity, long timestamp) {
        this.assetId = assetId;
        this.hazardType = Objects.requireNonNull(hazardType, "hazardType");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.reportedSeverity = reportedSeverity;
        this.timestamp = timestamp;
    }

    public int getAssetId() { return assetId; }
    public HazardType getHazardType() { return hazardType; }
    public SourceType getSourceType() { return sourceType; }
    public int getReportedSeverity() { return reportedSeverity; }
    public long getTimestamp() { return timestamp; }
}