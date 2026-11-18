package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TacticalCentroidCalculatorTest {

    @Test
    public void testEmptyOrNullCollectionReturnsNull() {
        assertNull(TacticalCentroidCalculator.calculate(null));
        assertNull(TacticalCentroidCalculator.calculate(Collections.emptyList()));
    }

    @Test
    public void testSingleUnitCentroid() {
        TelemetryEntity e1 = new TelemetryEntity();
        e1.userId = 1001L;
        e1.latitude = 55.7500;
        e1.longitude = 37.6200;

        TacticalCentroidCalculator.CentroidResult result = TacticalCentroidCalculator.calculate(Collections.singletonList(e1));
        assertNotNull(result);
        assertEquals(55.7500, result.latitude, 0.0001);
        assertEquals(37.6200, result.longitude, 0.0001);
        assertEquals(0.0, result.dispersionRadiusMeters, 0.1);
        assertEquals(1, result.unitCount);
        assertNotNull(result.dispersionCirclePoints);
        assertEquals(37, result.dispersionCirclePoints.size()); // 36 intervals + 1 closing point
    }

    @Test
    public void testMultipleUnitsCentroidAndDispersion() {
        List<TelemetryEntity> list = new ArrayList<>();

        TelemetryEntity e1 = new TelemetryEntity();
        e1.userId = 1001L;
        e1.latitude = 55.7500;
        e1.longitude = 37.6100;
        list.add(e1);

        TelemetryEntity e2 = new TelemetryEntity();
        e2.userId = 1002L;
        e2.latitude = 55.7500;
        e2.longitude = 37.6300;
        list.add(e2);

        TacticalCentroidCalculator.CentroidResult result = TacticalCentroidCalculator.calculate(list);
        assertNotNull(result);
        assertEquals(55.7500, result.latitude, 0.0001);
        assertEquals(37.6200, result.longitude, 0.0001);
        assertEquals(2, result.unitCount);
        assertTrue("Dispersion radius should be greater than 0", result.dispersionRadiusMeters > 0);
        // Distance from 37.62 to 37.61 at lat 55.75 is approx 625 meters
        assertTrue("Dispersion radius should be approx 600m", result.dispersionRadiusMeters > 500 && result.dispersionRadiusMeters < 800);
    }

    @Test
    public void testZeroCoordinatesIgnored() {
        List<TelemetryEntity> list = new ArrayList<>();

        TelemetryEntity e1 = new TelemetryEntity();
        e1.userId = 1001L;
        e1.latitude = 55.7500;
        e1.longitude = 37.6200;
        list.add(e1);

        TelemetryEntity eInvalid = new TelemetryEntity();
        eInvalid.userId = 1002L;
        eInvalid.latitude = 0.0;
        eInvalid.longitude = 0.0;
        list.add(eInvalid);

        TacticalCentroidCalculator.CentroidResult result = TacticalCentroidCalculator.calculate(list);
        assertNotNull(result);
        assertEquals(1, result.unitCount);
        assertEquals(55.7500, result.latitude, 0.0001);
        assertEquals(37.6200, result.longitude, 0.0001);
    }

    private static TelemetryEntity at(double lat, double lon) {
        TelemetryEntity e = new TelemetryEntity();
        e.latitude = lat;
        e.longitude = lon;
        return e;
    }

    @Test
    public void minimumDispersionConstantIs20Meters() {
        assertEquals(20.0, TacticalCentroidCalculator.MIN_DISPERSION_RADIUS_METERS, 0.0);
    }

    @Test
    public void onlyZeroZeroUnitsGiveNull() {
        assertNull(TacticalCentroidCalculator.calculate(java.util.Arrays.asList(at(0, 0), at(0, 0))));
    }

    @Test
    public void nullElementsAreSkipped() {
        java.util.List<TelemetryEntity> list = new java.util.ArrayList<>();
        list.add(null);
        list.add(at(55.0, 37.0));
        list.add(null);
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(list);
        assertNotNull(r);
        assertEquals(1, r.unitCount);
    }

    @Test
    public void collectionOfOnlyNullsGivesNull() {
        assertNull(TacticalCentroidCalculator.calculate(java.util.Arrays.asList((TelemetryEntity) null, null)));
    }

    @Test
    public void centroidIsArithmeticMeanOfThreeUnits() {
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(
                java.util.Arrays.asList(at(55.0, 37.0), at(56.0, 38.0), at(57.0, 36.0)));
        assertEquals(56.0, r.latitude, 1e-9);
        assertEquals(37.0, r.longitude, 1e-9);
        assertEquals(3, r.unitCount);
    }

    @Test
    public void dispersionIsDistanceToFarthestUnit() {
        // Два бойца на меридиане, разнесены на 0.002° (~222 м): центроид посередине, радиус ~111 м.
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(
                java.util.Arrays.asList(at(55.000, 37.0), at(55.002, 37.0)));
        assertEquals(111.2, r.dispersionRadiusMeters, 1.0);
    }

    @Test
    public void ringForTinyGroupUsesMinimumRadius() {
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(
                java.util.Collections.singletonList(at(55.0, 37.0)));
        assertEquals(0.0, r.dispersionRadiusMeters, 0.01);
        for (TacticalRangeRingGenerator.GeoPoint p : r.dispersionCirclePoints) {
            double d = TacticalCentroidCalculator.calculateDistanceMeters(r.latitude, r.longitude, p.latitude, p.longitude);
            assertEquals(20.0, d, 0.5);
        }
    }

    @Test
    public void ringForLargeGroupUsesActualRadius() {
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(
                java.util.Arrays.asList(at(55.0, 37.0), at(55.01, 37.0)));
        TacticalRangeRingGenerator.GeoPoint p = r.dispersionCirclePoints.get(0);
        double d = TacticalCentroidCalculator.calculateDistanceMeters(r.latitude, r.longitude, p.latitude, p.longitude);
        assertEquals(r.dispersionRadiusMeters, d, 1.0);
    }

    @Test
    public void unitWithZeroLatitudeButNonZeroLongitudeIsValid() {
        TacticalCentroidCalculator.CentroidResult r = TacticalCentroidCalculator.calculate(
                java.util.Collections.singletonList(at(0.0, 10.0)));
        assertNotNull(r);
        assertEquals(10.0, r.longitude, 1e-9);
    }

    @Test
    public void distanceMatchesNavigationCalculator() {
        double a = TacticalCentroidCalculator.calculateDistanceMeters(55.0, 37.0, 56.0, 38.0);
        double b = TacticalNavigationCalculator.calculateDistanceMeters(55.0, 37.0, 56.0, 38.0);
        assertEquals(b, a, 1e-6);
        assertTrue(a > 0);
    }
}
