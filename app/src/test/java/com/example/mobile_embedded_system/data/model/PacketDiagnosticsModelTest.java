package com.example.mobile_embedded_system.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Модульное тестирование модели диагностики и статистики пакетов по ТЗ (§5.1, §21).
 */
public class PacketDiagnosticsModelTest {

    @Test
    public void testDiagnosticsModelInitialCountersZero() {
        PacketDiagnosticsModel model = new PacketDiagnosticsModel();

        assertEquals(0L, model.getTotalPacketsReceived());
        assertEquals(0L, model.getTelemetryPacketsCount());
        assertEquals(0L, model.getCommandPacketsCount());
        assertEquals(0L, model.getDuplicatesDroppedCount());
        assertEquals(0L, model.getDecryptionErrorsCount());
        assertEquals(0L, model.getMalformedPacketsCount());
        assertEquals(0L, model.getBindingMismatchErrorsCount());
        assertEquals(0L, model.getDirectWifiPacketsCount());
    }

    @Test
    public void testDiagnosticsModelIncrementAndReset() {
        PacketDiagnosticsModel model = new PacketDiagnosticsModel();

        model.incrementTotalPackets();
        model.incrementTelemetryPackets();
        model.incrementCommandPackets();
        model.incrementDuplicatesDropped();
        model.incrementDecryptionErrors();
        model.incrementMalformedPackets();
        model.incrementBindingMismatchErrors();
        model.incrementDirectWifiPackets();

        assertEquals(1L, model.getTotalPacketsReceived());
        assertEquals(1L, model.getTelemetryPacketsCount());
        assertEquals(1L, model.getCommandPacketsCount());
        assertEquals(1L, model.getDuplicatesDroppedCount());
        assertEquals(1L, model.getDecryptionErrorsCount());
        assertEquals(1L, model.getMalformedPacketsCount());
        assertEquals(1L, model.getBindingMismatchErrorsCount());
        assertEquals(1L, model.getDirectWifiPacketsCount());

        model.resetCounters();

        assertEquals(0L, model.getTotalPacketsReceived());
        assertEquals(0L, model.getTelemetryPacketsCount());
        assertEquals(0L, model.getCommandPacketsCount());
        assertEquals(0L, model.getDuplicatesDroppedCount());
        assertEquals(0L, model.getDecryptionErrorsCount());
        assertEquals(0L, model.getMalformedPacketsCount());
        assertEquals(0L, model.getBindingMismatchErrorsCount());
        assertEquals(0L, model.getDirectWifiPacketsCount());
    }

    @Test
    public void testDiagnosticsModelMqttProperties() {
        PacketDiagnosticsModel model = new PacketDiagnosticsModel();
        model.setBrokerUrl("tcp://test.broker:1883");
        model.setConnectionState("ПОДКЛЮЧЕНО");
        model.setActiveSubscriptionTopic("mesh-a/#");

        assertEquals("tcp://test.broker:1883", model.getBrokerUrl());
        assertEquals("ПОДКЛЮЧЕНО", model.getConnectionState());
        assertEquals("mesh-a/#", model.getActiveSubscriptionTopic());
    }
}
