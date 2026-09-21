package com.example.mobile_embedded_system.domain;

import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Калькулятор тактического центроида и круга рассредоточения подразделения (ТЗ Part D §6.9.3, §6.10).
 */
public final class TacticalCentroidCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    public static final double MIN_DISPERSION_RADIUS_METERS = 20.0;

    private TacticalCentroidCalculator() {
        // Utility class
    }

    public static class CentroidResult {
        public final double latitude;
        public final double longitude;
        public final double dispersionRadiusMeters;
        public final int unitCount;
        public final List<TacticalRangeRingGenerator.GeoPoint> dispersionCirclePoints;

        public CentroidResult(double latitude, double longitude, double dispersionRadiusMeters, int unitCount, List<TacticalRangeRingGenerator.GeoPoint> circlePoints) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.dispersionRadiusMeters = dispersionRadiusMeters;
            this.unitCount = unitCount;
            this.dispersionCirclePoints = circlePoints;
        }
    }

    /**
     * Вычисление тактического центроида группы бойцов и радиуса рассредоточения.
     * @param entities коллекция актуальных телеметрических данных бойцов подразделения
     * @return CentroidResult либо null, если нет активных бойцов с валидными координатами.
     */
    public static CentroidResult calculate(Collection<TelemetryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        List<TacticalRangeRingGenerator.GeoPoint> validPoints = new ArrayList<>();
        double sumLat = 0.0;
        double sumLon = 0.0;

        for (TelemetryEntity entity : entities) {
            if (entity != null && (entity.latitude != 0.0 || entity.longitude != 0.0)) {
                validPoints.add(new TacticalRangeRingGenerator.GeoPoint(entity.latitude, entity.longitude));
                sumLat += entity.latitude;
                sumLon += entity.longitude;
            }
        }

        if (validPoints.isEmpty()) {
            return null;
        }

        int count = validPoints.size();
        double centroidLat = sumLat / count;
        double centroidLon = sumLon / count;

        double maxDistance = 0.0;
        for (TacticalRangeRingGenerator.GeoPoint pt : validPoints) {
            double d = calculateDistanceMeters(centroidLat, centroidLon, pt.latitude, pt.longitude);
            if (d > maxDistance) {
                maxDistance = d;
            }
        }

        double dispersionRadius = Math.max(maxDistance, MIN_DISPERSION_RADIUS_METERS);
        List<TacticalRangeRingGenerator.GeoPoint> circlePoints =
                TacticalRangeRingGenerator.generateRingPoints(centroidLat, centroidLon, dispersionRadius);

        return new CentroidResult(centroidLat, centroidLon, maxDistance, count, circlePoints);
    }

    /**
     * Вычисление геодезического расстояния между двумя точками (в метрах) по формуле гаверсинусов.
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
