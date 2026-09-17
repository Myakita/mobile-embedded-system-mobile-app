package com.example.mobile_embedded_system.data.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Диагностическая модель статистики пакетного обмена и MQTT по ТЗ (§21, MVP §1.98–1.101).
 */
public class PacketDiagnosticsModel {

    // Счётчики пакетов (§1.100)
    private final AtomicLong totalPacketsReceived = new AtomicLong(0L);
    private final AtomicLong telemetryPacketsCount = new AtomicLong(0L);
    private final AtomicLong commandPacketsCount = new AtomicLong(0L);
    private final AtomicLong duplicatesDroppedCount = new AtomicLong(0L);
    private final AtomicLong decryptionErrorsCount = new AtomicLong(0L);
    private final AtomicLong malformedPacketsCount = new AtomicLong(0L);

    // Состояние MQTT (§1.99)
    private String brokerUrl = "tcp://broker.hivemq.com:1883";
    private String connectionState = "ОТКЛЮЧЕНО";
    private long lastReconnectTimestampMs = System.currentTimeMillis();
    private String activeSubscriptionTopic = "mesh-a/7F10/21A0/#";

    public PacketDiagnosticsModel() {
    }

    public void incrementTotalPackets() { totalPacketsReceived.incrementAndGet(); }
    public void incrementTelemetryPackets() { telemetryPacketsCount.incrementAndGet(); }
    public void incrementCommandPackets() { commandPacketsCount.incrementAndGet(); }
    public void incrementDuplicatesDropped() { duplicatesDroppedCount.incrementAndGet(); }
    public void incrementDecryptionErrors() { decryptionErrorsCount.incrementAndGet(); }
    public void incrementMalformedPackets() { malformedPacketsCount.incrementAndGet(); }

    public void resetCounters() {
        totalPacketsReceived.set(0L);
        telemetryPacketsCount.set(0L);
        commandPacketsCount.set(0L);
        duplicatesDroppedCount.set(0L);
        decryptionErrorsCount.set(0L);
        malformedPacketsCount.set(0L);
    }

    public long getTotalPacketsReceived() { return totalPacketsReceived.get(); }
    public long getTelemetryPacketsCount() { return telemetryPacketsCount.get(); }
    public long getCommandPacketsCount() { return commandPacketsCount.get(); }
    public long getDuplicatesDroppedCount() { return duplicatesDroppedCount.get(); }
    public long getDecryptionErrorsCount() { return decryptionErrorsCount.get(); }
    public long getMalformedPacketsCount() { return malformedPacketsCount.get(); }

    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

    public String getConnectionState() { return connectionState; }
    public void setConnectionState(String connectionState) { this.connectionState = connectionState; }

    public long getLastReconnectTimestampMs() { return lastReconnectTimestampMs; }
    public void setLastReconnectTimestampMs(long lastReconnectTimestampMs) { this.lastReconnectTimestampMs = lastReconnectTimestampMs; }

    public String getActiveSubscriptionTopic() { return activeSubscriptionTopic; }
    public void setActiveSubscriptionTopic(String activeSubscriptionTopic) { this.activeSubscriptionTopic = activeSubscriptionTopic; }
}
