package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Доменный менеджер эскалации аварийных ситуаций группы (ТЗ §4.2, §6.7, §6.8).
 */
public class SquadAlertManager {

    public static class AlertInfo {
        public final long userId;
        public final String reason;
        public final long timestampMs;
        public final boolean isNewAlert;

        public AlertInfo(long userId, String reason, long timestampMs, boolean isNewAlert) {
            this.userId = userId;
            this.reason = reason;
            this.timestampMs = timestampMs;
            this.isNewAlert = isNewAlert;
        }

        public AlertInfo(long userId, String reason, long timestampMs) {
            this(userId, reason, timestampMs, true);
        }
    }

    private final Map<Long, TacticalStatusEvaluator.Status> previousStatuses = new ConcurrentHashMap<>();

    /**
     * Обработка телеметрии бойца.
     * @return AlertInfo если зафиксирован критический статус, иначе null.
     */
    public AlertInfo processTelemetry(TelemetryEntity entity) {
        if (entity == null) {
            return null;
        }

        TacticalStatusEvaluator.Status previousStatus = previousStatuses.get(entity.userId);
        TacticalStatusEvaluator.Status currentStatus = TacticalStatusEvaluator.evaluate(
                entity.pulseBpm,
                entity.temperatureCelsius
        );

        previousStatuses.put(entity.userId, currentStatus);

        if (currentStatus == TacticalStatusEvaluator.Status.CRITICAL) {
            boolean isNewAlert = (previousStatus != TacticalStatusEvaluator.Status.CRITICAL);
            String reason = formatCriticalReason(entity.pulseBpm, entity.temperatureCelsius);
            return new AlertInfo(entity.userId, reason, entity.receivedAtMs, isNewAlert);
        }

        return null;
    }

    /**
     * Проверяет, есть ли хотя бы один боец с активным критическим статусом.
     */
    public boolean hasActiveCriticalAlert() {
        return previousStatuses.containsValue(TacticalStatusEvaluator.Status.CRITICAL);
    }

    private String formatCriticalReason(int pulse, double temp) {
        if (pulse > 120) {
            return "ТАХИКАРДИЯ (" + pulse + " BPM)";
        }
        if (pulse < 45) {
            return "БРАДИКАРДИЯ / ШОК (" + pulse + " BPM)";
        }
        if (temp > 38.5) {
            return "ГИПЕРТЕРМИЯ (" + String.format(java.util.Locale.US, "%.1f", temp) + " °C)";
        }
        return "КРИТИЧЕСКИЕ ПОКАЗАТЕЛИ";
    }
}