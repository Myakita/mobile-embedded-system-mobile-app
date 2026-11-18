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

    private static TelemetryEntity validFix(long timestamp) {
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = 55.753912;
        e.longitude = 37.620811;
        e.positionQuality = 3;
        e.timestamp = timestamp;
        return e;
    }

    @Test
    public void thresholdConstantIs30Seconds() {
        assertEquals(30L, PositionStatusEvaluator.STALE_THRESHOLD_SECONDS);
    }

    @Test
    public void ageExactly30SecondsIsCurrent() {
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT,
                PositionStatusEvaluator.evaluate(validFix(1000L), 1030L));
    }

    @Test
    public void ageExactly31SecondsIsStale() {
        assertEquals(PositionStatusEvaluator.PositionState.STALE,
                PositionStatusEvaluator.evaluate(validFix(1000L), 1031L));
    }

    @Test
    public void sameFixBecomesStaleWithoutNewPackets() {
        // AC-04.4: устаревание происходит только за счёт хода времени.
        TelemetryEntity fix = validFix(1000L);
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT, PositionStatusEvaluator.evaluate(fix, 1005L));
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT, PositionStatusEvaluator.evaluate(fix, 1030L));
        assertEquals(PositionStatusEvaluator.PositionState.STALE, PositionStatusEvaluator.evaluate(fix, 1031L));
    }

    @Test
    public void zeroZeroWithGoodQualityIsUnknown() {
        // AC-04.5: точка 0°, 0° не считается валидной позицией.
        TelemetryEntity e = validFix(1000L);
        e.latitude = 0.0;
        e.longitude = 0.0;
        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN, PositionStatusEvaluator.evaluate(e, 1000L));
    }

    @Test
    public void coordinatesBelowEpsilonAreUnknown() {
        TelemetryEntity e = validFix(1000L);
        e.latitude = 1e-7;
        e.longitude = -1e-7;
        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN, PositionStatusEvaluator.evaluate(e, 1000L));
    }

    @Test
    public void zeroLatitudeAloneIsStillValid() {
        TelemetryEntity e = validFix(1000L);
        e.latitude = 0.0;
        e.longitude = 10.0;
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT, PositionStatusEvaluator.evaluate(e, 1000L));
    }

    @Test
    public void lowQualityDoesNotMakePositionStale() {
        // Качество позиции показывается отдельно от её возраста.
        TelemetryEntity e = validFix(1000L);
        e.positionQuality = 1;
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT, PositionStatusEvaluator.evaluate(e, 1010L));
        assertEquals(PositionStatusEvaluator.PositionState.STALE, PositionStatusEvaluator.evaluate(e, 1100L));
    }

    @Test
    public void zeroQualityIsUnknownEvenWhenFresh() {
        TelemetryEntity e = validFix(1000L);
        e.positionQuality = 0;
        assertEquals(PositionStatusEvaluator.PositionState.UNKNOWN, PositionStatusEvaluator.evaluate(e, 1000L));
    }

    @Test
    public void smallClockSkewIntoFutureIsCurrent() {
        assertEquals(PositionStatusEvaluator.PositionState.CURRENT,
                PositionStatusEvaluator.evaluate(validFix(1002L), 1000L));
    }

    @Test
    public void ageCalculationNullIsMinusOne() {
        assertEquals(-1L, PositionStatusEvaluator.calculateAgeSeconds(null, 1000L));
    }

    @Test
    public void ageCalculationReturnsDifference() {
        assertEquals(45L, PositionStatusEvaluator.calculateAgeSeconds(validFix(1000L), 1045L));
        assertEquals(0L, PositionStatusEvaluator.calculateAgeSeconds(validFix(1000L), 1000L));
    }

    @Test
    public void ageCalculationClampsFutureTimestampToZero() {
        assertEquals(0L, PositionStatusEvaluator.calculateAgeSeconds(validFix(1500L), 1000L));
    }

    @Test
    public void stateLabelsAreRussianUiStrings() {
        assertEquals("АКТУАЛЬНО", PositionStatusEvaluator.PositionState.CURRENT.getLabel());
        assertEquals("УСТАРЕЛО", PositionStatusEvaluator.PositionState.STALE.getLabel());
        assertEquals("НЕДОСТУПНО", PositionStatusEvaluator.PositionState.UNKNOWN.getLabel());
    }
}
