package com.trident.harbour.service;

import com.trident.harbour.model.EventReport;
import com.trident.harbour.model.HazardType;
import com.trident.harbour.model.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfidenceScorerTest {

    private final ConfidenceScorer scorer = new ConfidenceScorer();

    private EventReport report(SourceType type) {
        return new EventReport(1, HazardType.values()[0], type, 3, 0L);
    }

    @Test
    void singleWeatherReportScoresLow() {
        double c = scorer.score(List.of(report(SourceType.WEATHER_FEED)));
        assertEquals(0.5, c, 0.001);
    }

    @Test
    void sensorPlusCrewCorroborate() {
        double c = scorer.score(List.of(report(SourceType.SENSOR), report(SourceType.CREW)));
        assertEquals(0.97, c, 0.001);
    }

    @Test
    void noReportsScoresZero() {
        assertEquals(0.0, scorer.score(List.of()), 0.001);
    }

    @Test
    void duplicateSourceDoesNotInflate() {
        double c = scorer.score(List.of(
                report(SourceType.WEATHER_FEED),
                report(SourceType.WEATHER_FEED),
                report(SourceType.WEATHER_FEED)));
        assertEquals(0.5, c, 0.001);
    }
}