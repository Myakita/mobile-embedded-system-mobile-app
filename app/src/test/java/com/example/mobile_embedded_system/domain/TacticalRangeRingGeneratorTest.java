package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Тестирование точности генерации колец дальности (ТЗ §5.1, §6.10).
 */
public class TacticalRangeRingGeneratorTest {

    private static final double DELTA_DISTANCE = 1.0; // Погрешность до 1 метра

    @Test
    public void testRingIsClosed() {
        double lat = 55.7539;
        double lon = 37.6208;
        List<TacticalRangeRingGenerator.GeoPoint> ring = TacticalRangeRingGenerator.generateRingPoints(lat, lon, 100.0);

        assertFalse(ring.isEmpty());
        TacticalRangeRingGenerator.GeoPoint first = ring.get(0);
        TacticalRangeRingGenerator.GeoPoint last = ring.get(ring.size() - 1);

        assertEquals(first.latitude, last.latitude, 0.000001);
        assertEquals(first.longitude, last.longitude, 0.000001);
    }

    @Test
    public void testRingRadiusAccuracy() {
        double centerLat = 55.7539;
        double centerLon = 37.6208;
        double targetRadius = 250.0;

        List<TacticalRangeRingGenerator.GeoPoint> ring = TacticalRangeRingGenerator.generateRingPoints(centerLat, centerLon, targetRadius);

        for (TacticalRangeRingGenerator.GeoPoint point : ring) {
            double actualDistance = TacticalNavigationCalculator.calculateDistanceMeters(
                    centerLat, centerLon, point.latitude, point.longitude
            );
            assertEquals(targetRadius, actualDistance, DELTA_DISTANCE);
        }
    }

    @Test
    public void defaultRingHas37PointsEvery10Degrees() {
        assertEquals(37, TacticalRangeRingGenerator.generateRingPoints(55.0, 37.0, 100.0).size());
    }

    @Test
    public void customPointCountGivesCountPlusOne() {
        assertEquals(5, TacticalRangeRingGenerator.generateRingPoints(55.0, 37.0, 100.0, 4).size());
        assertEquals(2, TacticalRangeRingGenerator.generateRingPoints(55.0, 37.0, 100.0, 1).size());
    }

    @Test
    public void cardinalPointsAreNorthEastSouthWest() {
        double lat = 10.0, lon = 20.0;
        java.util.List<TacticalRangeRingGenerator.GeoPoint> r =
                TacticalRangeRingGenerator.generateRingPoints(lat, lon, 1000.0, 4);
        assertTrue(r.get(0).latitude > lat);                      // север
        assertEquals(lon, r.get(0).longitude, 1e-9);
        assertTrue(r.get(1).longitude > lon);                     // восток
        assertEquals(lat, r.get(1).latitude, 1e-4);
        assertTrue(r.get(2).latitude < lat);                      // юг
        assertTrue(r.get(3).longitude < lon);                     // запад
    }

    @Test
    public void zeroRadiusCollapsesToCenter() {
        for (TacticalRangeRingGenerator.GeoPoint p : TacticalRangeRingGenerator.generateRingPoints(55.0, 37.0, 0.0)) {
            assertEquals(55.0, p.latitude, 1e-9);
            assertEquals(37.0, p.longitude, 1e-9);
        }
    }

    @Test
    public void radiusIsAccurateInSouthernWesternHemisphereAndHighLatitude() {
        double[][] centers = {{-33.8688, -70.6483}, {80.0, 10.0}, {0.0, 179.9}};
        for (double[] c : centers) {
            for (TacticalRangeRingGenerator.GeoPoint p : TacticalRangeRingGenerator.generateRingPoints(c[0], c[1], 1000.0)) {
                double d = TacticalNavigationCalculator.calculateDistanceMeters(c[0], c[1], p.latitude, p.longitude);
                assertEquals(1000.0, d, 1.0);
            }
        }
    }
}
