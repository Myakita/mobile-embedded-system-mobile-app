package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Test;

public class PositionStatusEvaluatorTest {

    @Test
    public void testNullEntityReturnsUnknown() {
        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN,
                PositionStatusEvaluator.evaluate(null, 1726000000L));
    }

    @Test
    public void testZeroCoordinatesReturnsUnknown() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.latitude = 0.0;
        entity.longitude = 0.0;
        entity.positionQuality = 3;
        entity.timestamp = 1726000000L;

        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN,
                PositionStatusEvaluator.evaluate(entity, 1726000000L));
    }

    @Test
    public void testZeroQualityReturnsUnknown() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.latitude = 55.753912;
        entity.longitude = 37.620811;
        entity.positionQuality = 0; // Нет решения
        entity.timestamp = 1726000000L;

        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN,
                PositionStatusEvaluator.evaluate(entity, 1726000010L));
    }

    @Test
    public void testAgeWithin30SecondsReturnsCurrent() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.latitude = 55.753912;
        entity.longitude = 37.620811;
        entity.positionQuality = 3;
        entity.timestamp = 1726000000L;

        // Возраст 0 с
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT,
                PositionStatusEvaluator.evaluate(entity, 1726000000L));

        // Возраст 30 с
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT,
                PositionStatusEvaluator.evaluate(entity, 1726000030L));
    }

    @Test
    public void testAgeOlderThan30SecondsReturnsStale() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.latitude = 55.753912;
        entity.longitude = 37.620811;
        entity.positionQuality = 3;
        entity.timestamp = 1726000000L;

        // Возраст 31 с
        assertEquals(PositionStatusEvaluator.PositionState.STALE,
                PositionStatusEvaluator.evaluate(entity, 1726000031L));

        // Возраст 120 с
        assertEquals(PositionStatusEvaluator.PositionState.STALE,
                PositionStatusEvaluator.evaluate(entity, 1726000120L));
    }
}
