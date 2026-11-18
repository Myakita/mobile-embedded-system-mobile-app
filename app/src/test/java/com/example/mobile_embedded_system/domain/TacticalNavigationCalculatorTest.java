package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Тестирование расчётов дистанции и пеленга (ТЗ §5.1, §6.10).
 */
public class TacticalNavigationCalculatorTest {

    private static final double DELTA_DISTANCE = 1.0; // точность до 1 метра
    private static final double DELTA_BEARING = 0.5;  // точность до 0.5 градуса

    @Test
    public void testDistanceToSamePointIsZero() {
        double dist = TacticalNavigationCalculator.calculateDistanceMeters(55.7539, 37.6208, 55.7539, 37.6208);
        assertEquals(0.0, dist, DELTA_DISTANCE);
    }

    @Test
    public void testBearingDirectNorth() {
        // Точка строго на север: широта увеличивается, долгота та же
        double bearing = TacticalNavigationCalculator.calculateBearingDegrees(55.0, 37.0, 56.0, 37.0);
        assertEquals(0.0, bearing, DELTA_BEARING);
    }

    @Test
    public void testBearingDirectEast() {
        // Точка строго на восток на экваторе
        double bearing = TacticalNavigationCalculator.calculateBearingDegrees(0.0, 0.0, 0.0, 1.0);
        assertEquals(90.0, bearing, DELTA_BEARING);
    }

    @Test
    public void testBearingDirectSouth() {
        // Точка строго на юг
        double bearing = TacticalNavigationCalculator.calculateBearingDegrees(56.0, 37.0, 55.0, 37.0);
        assertEquals(180.0, bearing, DELTA_BEARING);
    }

    @Test
    public void testBearingDirectWest() {
        // Точка строго на запад на экваторе
        double bearing = TacticalNavigationCalculator.calculateBearingDegrees(0.0, 1.0, 0.0, 0.0);
        assertEquals(270.0, bearing, DELTA_BEARING);
    }

    @Test
    public void testKnownDistanceApproximation() {
        // 1 градус по меридиану ~ 111.195 км
        double dist = TacticalNavigationCalculator.calculateDistanceMeters(55.0, 37.0, 56.0, 37.0);
        assertEquals(111195.0, dist, 200.0);
    }

    @Test
    public void equatorOneDegreeLongitudeIs111km() {
        assertEquals(111195.0, TacticalNavigationCalculator.calculateDistanceMeters(0, 0, 0, 1), 5.0);
    }

    @Test
    public void halfEarthCircumferenceBetweenAntipodes() {
        assertEquals(Math.PI * 6371000.0, TacticalNavigationCalculator.calculateDistanceMeters(0, 0, 0, 180), 1.0);
    }

    @Test
    public void distanceIsSymmetric() {
        double ab = TacticalNavigationCalculator.calculateDistanceMeters(55.7539, 37.6208, 59.9343, 30.3351);
        double ba = TacticalNavigationCalculator.calculateDistanceMeters(59.9343, 30.3351, 55.7539, 37.6208);
        assertEquals(ab, ba, 1e-6);
        assertTrue(ab > 600_000 && ab < 700_000);
    }

    @Test
    public void distanceAcrossPoleIsTwoDegreesOfArc() {
        assertEquals(222390.0, TacticalNavigationCalculator.calculateDistanceMeters(89, 0, 89, 180), 10.0);
    }

    @Test
    public void distanceAcrossAntimeridianIsShort() {
        assertEquals(111195.0, TacticalNavigationCalculator.calculateDistanceMeters(0, 179.5, 0, -179.5), 5.0);
    }

    @Test
    public void bearingNorthEastIsAboutFortyFiveAtEquator() {
        assertEquals(45.0, TacticalNavigationCalculator.calculateBearingDegrees(0, 0, 1, 1), 0.1);
    }

    @Test
    public void bearingAcrossAntimeridianEastAndWest() {
        assertEquals(90.0, TacticalNavigationCalculator.calculateBearingDegrees(0, 179.5, 0, -179.5), 0.01);
        assertEquals(270.0, TacticalNavigationCalculator.calculateBearingDegrees(0, -179.5, 0, 179.5), 0.01);
    }

    @Test
    public void bearingIsAlwaysInZeroTo360Range() {
        for (int lat = -80; lat <= 80; lat += 20) {
            for (int lon = -170; lon <= 170; lon += 30) {
                double b = TacticalNavigationCalculator.calculateBearingDegrees(10, 10, lat, lon);
                assertTrue("bearing " + b, b >= 0.0 && b < 360.0);
            }
        }
    }

    @Test
    public void bearingToSamePointIsZero() {
        assertEquals(0.0, TacticalNavigationCalculator.calculateBearingDegrees(55.0, 37.0, 55.0, 37.0), 1e-9);
    }

    @Test
    public void bearingSouthWestIsInThirdQuadrant() {
        double b = TacticalNavigationCalculator.calculateBearingDegrees(0, 0, -1, -1);
        assertEquals(225.0, b, 0.1);
    }
}
