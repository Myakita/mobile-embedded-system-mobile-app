package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Тестирование менеджера тактических ориентиров и назначения целей (ТЗ §5.1, §6.10, §6.13).
 */
public class TacticalWaypointManagerTest {

    private TacticalWaypointManager manager;

    @Before
    public void setUp() {
        manager = new TacticalWaypointManager();
    }

    @Test
    public void testSequentialNaming() {
        Waypoint wp1 = manager.addWaypoint(55.75, 37.61);
        Waypoint wp2 = manager.addWaypoint(55.76, 37.62);

        assertEquals("ОБУ-1", wp1.getCallsign());
        assertEquals("ОБУ-2", wp2.getCallsign());
        assertEquals(2, manager.getWaypoints().size());
    }

    @Test
    public void testUnitTargetAssignmentPersistence() {
        Waypoint targetShooter = manager.addWaypoint(55.755, 37.625);
        Waypoint targetMedic = manager.addWaypoint(55.758, 37.629);

        manager.assignTargetToUnit(1002L, targetShooter.getId());
        manager.assignTargetToUnit(1003L, targetMedic.getId());

        // Проверяем, что цели закреплены независимо за каждым бойцом
        Waypoint retrieved1002 = manager.getAssignedWaypointForUnit(1002L);
        Waypoint retrieved1003 = manager.getAssignedWaypointForUnit(1003L);

        assertNotNull(retrieved1002);
        assertNotNull(retrieved1003);
        assertEquals(targetShooter.getId(), retrieved1002.getId());
        assertEquals(targetMedic.getId(), retrieved1003.getId());

        // Проверяем удаление
        manager.clearTargetForUnit(1002L);
        assertNull(manager.getAssignedWaypointForUnit(1002L));
        assertNotNull(manager.getAssignedWaypointForUnit(1003L));
    }

    @Test
    public void testNavCalculation() {
        Waypoint target = manager.addWaypoint(56.0, 37.0);
        TacticalWaypointManager.NavInfo nav = manager.calculateNav(55.0, 37.0, target);

        assertNotNull(nav);
        assertEquals(0.0, nav.bearingDegrees, 0.5);
        assertEquals(111195.0, nav.distanceMeters, 200.0);
    }

    @Test
    public void newManagerHasNoWaypoints() {
        assertTrue(manager.getWaypoints().isEmpty());
    }

    @Test
    public void waypointKeepsCoordinatesAndCreationTime() {
        long before = System.currentTimeMillis();
        Waypoint wp = manager.addWaypoint(-12.5, 130.25);
        assertEquals(1L, wp.getId());
        assertEquals(-12.5, wp.getLatitude(), 0.0);
        assertEquals(130.25, wp.getLongitude(), 0.0);
        assertTrue(wp.getTimestampMs() >= before);
    }

    @Test
    public void idsAreNotReusedAfterRemoval() {
        Waypoint a = manager.addWaypoint(1, 1);
        manager.addWaypoint(2, 2);
        assertTrue(manager.removeWaypoint(a.getId()));
        Waypoint c = manager.addWaypoint(3, 3);
        assertEquals(3L, c.getId());
        assertEquals("ОБУ-3", c.getCallsign());
    }

    @Test
    public void removeUnknownWaypointReturnsFalse() {
        manager.addWaypoint(1, 1);
        assertFalse(manager.removeWaypoint(999L));
        assertEquals(1, manager.getWaypoints().size());
    }

    @Test
    public void removingWaypointClearsUnitAssignment() {
        Waypoint wp = manager.addWaypoint(1, 1);
        manager.assignTargetToUnit(1002L, wp.getId());
        manager.removeWaypoint(wp.getId());
        assertNull(manager.getAssignedWaypointForUnit(1002L));
        assertNull(manager.getUnitAssignedToWaypoint(wp.getId()));
    }

    @Test
    public void reassignmentReplacesPreviousTarget() {
        Waypoint a = manager.addWaypoint(1, 1);
        Waypoint b = manager.addWaypoint(2, 2);
        manager.assignTargetToUnit(1002L, a.getId());
        manager.assignTargetToUnit(1002L, b.getId());
        assertEquals(b.getId(), manager.getAssignedWaypointForUnit(1002L).getId());
        assertNull(manager.getUnitAssignedToWaypoint(a.getId()));
    }

    @Test
    public void unitAssignedToWaypointIsFound() {
        Waypoint a = manager.addWaypoint(1, 1);
        manager.assignTargetToUnit(1003L, a.getId());
        assertEquals(Long.valueOf(1003L), manager.getUnitAssignedToWaypoint(a.getId()));
    }

    @Test
    public void assignmentToUnknownWaypointYieldsNoTarget() {
        manager.assignTargetToUnit(1002L, 42L);
        assertNull(manager.getAssignedWaypointForUnit(1002L));
    }

    @Test
    public void unitWithoutAssignmentHasNoTarget() {
        assertNull(manager.getAssignedWaypointForUnit(1002L));
        manager.clearTargetForUnit(1002L); // не должно падать
    }

    @Test(expected = UnsupportedOperationException.class)
    public void waypointListIsReadOnly() {
        manager.addWaypoint(1, 1);
        manager.getWaypoints().clear();
    }

    @Test
    public void waypointListIsSnapshot() {
        manager.addWaypoint(1, 1);
        java.util.List<Waypoint> snapshot = manager.getWaypoints();
        manager.addWaypoint(2, 2);
        assertEquals(1, snapshot.size());
        assertEquals(2, manager.getWaypoints().size());
    }

    @Test
    public void navToNullTargetIsNull() {
        assertNull(manager.calculateNav(55.0, 37.0, null));
    }

    @Test
    public void navToSamePointIsZeroDistance() {
        Waypoint wp = manager.addWaypoint(55.0, 37.0);
        TacticalWaypointManager.NavInfo nav = manager.calculateNav(55.0, 37.0, wp);
        assertNotNull(nav);
        assertEquals(0.0, nav.distanceMeters, 0.001);
    }

    @Test
    public void navBearingToEast() {
        Waypoint wp = manager.addWaypoint(0.0, 1.0);
        TacticalWaypointManager.NavInfo nav = manager.calculateNav(0.0, 0.0, wp);
        assertEquals(90.0, nav.bearingDegrees, 0.01);
        assertEquals(111195.0, nav.distanceMeters, 5.0);
    }
}
