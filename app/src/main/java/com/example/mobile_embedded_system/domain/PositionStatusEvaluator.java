package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

/**
 * Оценщик состояния актуальности геопозиции по ТЗ (§13, AC-04.4).
 * - CURRENT: возраст фикса <= 30 секунд
 * - STALE: возраст фикса > 30 секунд
 * - UNKNOWN: нет решения (positionQuality == 0, либо координаты не валидны / отсутствуют)
 */
public final class PositionStatusEvaluator {

    public static final long STALE_THRESHOLD_SECONDS = 30L;

    public enum PositionState {
        CURRENT("АКТУАЛЬНО"),
        STALE("УСТАРЕЛО"),
        UNKNOWN("НЕДОСТУПНО");

        private final String label;

        PositionState(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private PositionStatusEvaluator() {
    }

    public static PositionState evaluate(TelemetryEntity entity, long nowSec) {
        if (entity == null) {
            return PositionState.UNKNOWN;
        }
        // Если качество позиции 0 (недоступно) или координаты отсутствуют / 0.0 (AC-04.5)
        if (entity.positionQuality == 0 || (Math.abs(entity.latitude) < 1e-6 && Math.abs(entity.longitude) < 1e-6)) {
            return PositionState.UNKNOWN;
        }

        long ageSec = nowSec - entity.timestamp;
        if (ageSec < 0) {
            // Допуск на минимальную рассинхронизацию часов стенда
            return PositionState.CURRENT;
        }

        return (ageSec <= STALE_THRESHOLD_SECONDS) ? PositionState.CURRENT : PositionState.STALE;
    }

    public static long calculateAgeSeconds(TelemetryEntity entity, long nowSec) {
        if (entity == null) return -1L;
        return Math.max(0L, nowSec - entity.timestamp);
    }
}
