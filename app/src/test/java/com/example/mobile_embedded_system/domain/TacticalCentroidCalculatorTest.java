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
}
