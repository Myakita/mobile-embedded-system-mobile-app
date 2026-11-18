package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Before;
import org.junit.Test;

/**
 * Тестирование менеджера тревог и эскалации (ТЗ §5.1, §6.7).
 */
public class SquadAlertManagerTest {

    private SquadAlertManager alertManager;

    @Before
    public void setUp() {
        alertManager = new SquadAlertManager();
    }

    @Test
    public void testNominalTelemetryDoesNotTriggerAlert() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.userId = 1001L;
        entity.pulseBpm = 75;
        entity.temperatureCelsius = 36.6;

        SquadAlertManager.AlertInfo alert = alertManager.processTelemetry(entity);
        assertNull(alert);
        assertFalse(alertManager.hasActiveCriticalAlert());
    }

    @Test
    public void testCriticalPulseTriggersAlert() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.userId = 1002L;
        entity.pulseBpm = 135;
        entity.temperatureCelsius = 37.0;

        SquadAlertManager.AlertInfo alert = alertManager.processTelemetry(entity);
        assertNotNull(alert);
        assertEquals(1002L, alert.userId);
        assertTrue(alert.reason.contains("ТАХИКАРДИЯ"));
        assertTrue(alertManager.hasActiveCriticalAlert());
    }

    @Test
    public void testRecoveryClearsActiveCriticalState() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.userId = 1002L;
        entity.pulseBpm = 135;
        entity.temperatureCelsius = 37.0;
        alertManager.processTelemetry(entity);
        assertTrue(alertManager.hasActiveCriticalAlert());

        // Нормализация показателей
        entity.pulseBpm = 80;
        alertManager.processTelemetry(entity);
        assertFalse(alertManager.hasActiveCriticalAlert());
    }

    private static TelemetryEntity soldier(long id, int pulse, double temp) {
        TelemetryEntity e = new TelemetryEntity();
        e.userId = id;
        e.pulseBpm = pulse;
        e.temperatureCelsius = temp;
        e.receivedAtMs = 5555L;
        return e;
    }

    @Test
    public void nullTelemetryIsIgnored() {
        assertNull(alertManager.processTelemetry(null));
        assertFalse(alertManager.hasActiveCriticalAlert());
    }

    @Test
    public void warningLevelDoesNotCreateAlert() {
        assertNull(alertManager.processTelemetry(soldier(1001L, 100, 37.5)));
        assertFalse(alertManager.hasActiveCriticalAlert());
    }

    @Test
    public void bradycardiaReasonText() {
        SquadAlertManager.AlertInfo a = alertManager.processTelemetry(soldier(1001L, 40, 36.6));
        assertNotNull(a);
        assertEquals("БРАДИКАРДИЯ / ШОК (40 BPM)", a.reason);
    }

    @Test
    public void tachycardiaReasonText() {
        SquadAlertManager.AlertInfo a = alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        assertEquals("ТАХИКАРДИЯ (135 BPM)", a.reason);
    }

    @Test
    public void hyperthermiaReasonTextUsesDotDecimal() {
        SquadAlertManager.AlertInfo a = alertManager.processTelemetry(soldier(1001L, 80, 39.04));
        assertEquals("ГИПЕРТЕРМИЯ (39.0 °C)", a.reason);
    }

    @Test
    public void pulseReasonHasPriorityOverTemperature() {
        SquadAlertManager.AlertInfo a = alertManager.processTelemetry(soldier(1001L, 130, 40.0));
        assertTrue(a.reason.startsWith("ТАХИКАРДИЯ"));
    }

    @Test
    public void firstCriticalIsNewAndRepeatIsNot() {
        SquadAlertManager.AlertInfo first = alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        SquadAlertManager.AlertInfo second = alertManager.processTelemetry(soldier(1001L, 140, 36.6));
        assertTrue(first.isNewAlert);
        assertFalse(second.isNewAlert);
    }

    @Test
    public void alertIsNewAgainAfterRecovery() {
        alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        alertManager.processTelemetry(soldier(1001L, 70, 36.6));
        SquadAlertManager.AlertInfo again = alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        assertTrue(again.isNewAlert);
    }

    @Test
    public void warningBetweenCriticalsResetsNewFlag() {
        alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        alertManager.processTelemetry(soldier(1001L, 100, 36.6)); // WARNING
        assertTrue(alertManager.processTelemetry(soldier(1001L, 135, 36.6)).isNewAlert);
    }

    @Test
    public void alertCarriesUserAndReceiveTime() {
        SquadAlertManager.AlertInfo a = alertManager.processTelemetry(soldier(1003L, 135, 36.6));
        assertEquals(1003L, a.userId);
        assertEquals(5555L, a.timestampMs);
    }

    @Test
    public void soldiersAreTrackedIndependently() {
        assertTrue(alertManager.processTelemetry(soldier(1001L, 135, 36.6)).isNewAlert);
        assertTrue(alertManager.processTelemetry(soldier(1002L, 135, 36.6)).isNewAlert);
    }

    @Test
    public void activeAlertRemainsWhileAnotherSoldierIsCritical() {
        alertManager.processTelemetry(soldier(1001L, 135, 36.6));
        alertManager.processTelemetry(soldier(1002L, 135, 36.6));
        alertManager.processTelemetry(soldier(1001L, 70, 36.6));
        assertTrue(alertManager.hasActiveCriticalAlert());
        alertManager.processTelemetry(soldier(1002L, 70, 36.6));
        assertFalse(alertManager.hasActiveCriticalAlert());
    }

    @Test
    public void alertInfoThreeArgConstructorMarksNew() {
        SquadAlertManager.AlertInfo a = new SquadAlertManager.AlertInfo(7L, "x", 9L);
        assertTrue(a.isNewAlert);
        assertEquals(7L, a.userId);
    }

    @Test
    public void noAlertsInitially() {
        assertFalse(new SquadAlertManager().hasActiveCriticalAlert());
    }
}
