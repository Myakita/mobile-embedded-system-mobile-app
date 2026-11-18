package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Модульные тесты тактической путевой точки (ТЗ §6.10, §6.13).
 */
public class WaypointTest {

    @Test
    public void waypointIsImmutableValueHolder() {
        Waypoint wp = new Waypoint(3L, "ОБУ-3", -12.5, 130.25, 1700000000000L);
        assertEquals(3L, wp.getId());
        assertEquals("ОБУ-3", wp.getCallsign());
        assertEquals(-12.5, wp.getLatitude(), 0.0);
        assertEquals(130.25, wp.getLongitude(), 0.0);
        assertEquals(1700000000000L, wp.getTimestampMs());
    }

    @Test
    public void waypointAcceptsExtremeCoordinates() {
        Waypoint wp = new Waypoint(1L, "N", 90.0, -180.0, 0L);
        assertEquals(90.0, wp.getLatitude(), 0.0);
        assertEquals(-180.0, wp.getLongitude(), 0.0);
        assertEquals(0L, wp.getTimestampMs());
    }
}
