package com.example.mobile_embedded_system.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.mobile_embedded_system.data.model.LMashPayload;

import org.junit.Test;

/**
 * Модульный тест проверки формирования записи БД из сетевого пакета LMashPayload (ТЗ §13, §14).
 * Проверяет корректность типов, масштабирование величин и ключи дедупликации.
 */
public class TelemetryEntityTest {

    @Test
    public void testEntityConversionFromPayload() {
        LMashPayload payload = new LMashPayload();
        payload.setDeviceSerial(0x19AA0221L);
        payload.setSequence(471L);
        payload.setUserId(1001L);
        payload.setDestinationId(0x7A831204L);
        payload.setTimestamp(1726000000L);
        payload.setTemperatureX100((short) 3672); // 36.72 °C
        payload.setPulseBpm(84);
        payload.setPressureSys(120);
        payload.setPressureDia(80);
        payload.setLatitudeE7(557539123);
        payload.setLongitudeE7(376208110);
        payload.setHeadingCdeg(18050);
        payload.setPositionQuality(3);

        long beforeConversionMs = System.currentTimeMillis();
        TelemetryEntity entity = TelemetryEntity.fromPayload(payload);

        // Проверка составного первичного ключа дедупликации (deviceSerial, sequence)
        assertEquals(0x19AA0221L, entity.deviceSerial);
        assertEquals(471L, entity.sequence);

        // Проверка маршрутных полей
        assertEquals(1001L, entity.userId);
        assertEquals(0x7A831204L, entity.destinationId);
        assertEquals(1726000000L, entity.timestamp);

        // Проверка физиологических показателей
        assertEquals(36.72, entity.temperatureCelsius, 0.001);
        assertEquals(84, entity.pulseBpm);
        assertEquals(120, entity.pressureSys);
        assertEquals(80, entity.pressureDia);

        // Проверка координат и навигации
        assertEquals(55.7539123, entity.latitude, 0.0000001);
        assertEquals(37.6208110, entity.longitude, 0.0000001);
        assertEquals(180.50, entity.headingDegrees, 0.01);
        assertEquals(3, entity.positionQuality);

        // Метка времени приема
        assertTrue(entity.receivedAtMs >= beforeConversionMs);
    }

    @Test
    public void defaultNetworkIsMeshA() {
        assertEquals("mesh-a", new TelemetryEntity().networkId);
    }

    @Test
    public void negativeCoordinatesAndTemperatureAreConverted() {
        LMashPayload p = new LMashPayload();
        p.setLatitudeE7(-335000000);
        p.setLongitudeE7(-702500000);
        p.setTemperatureX100((short) -1050);
        TelemetryEntity e = TelemetryEntity.fromPayload(p);
        assertEquals(-33.5, e.latitude, 1e-9);
        assertEquals(-70.25, e.longitude, 1e-9);
        assertEquals(-10.5, e.temperatureCelsius, 1e-9);
    }

    @Test
    public void unsignedLimitsStayPositive() {
        LMashPayload p = new LMashPayload();
        p.setSequence(4294967295L);
        p.setTimestamp(4294967295L);
        p.setPulseBpm(65535);
        TelemetryEntity e = TelemetryEntity.fromPayload(p);
        assertEquals(4294967295L, e.sequence);
        assertEquals(4294967295L, e.timestamp);
        assertEquals(65535, e.pulseBpm);
    }

    @Test
    public void zeroPositionPayloadKeepsZeroQualityAndCoordinates() {
        TelemetryEntity e = TelemetryEntity.fromPayload(new LMashPayload());
        assertEquals(0, e.positionQuality);
        assertEquals(0.0, e.latitude, 0.0);
        assertEquals(0.0, e.longitude, 0.0);
    }

    @Test
    public void entityFromParsedWireBytesKeepsDedupKey() {
        LMashPayload p = LMashPayload.createCommand(102L, 1726000050L, 430572065L, 1001L, 2055410180L, LMashPayload.CMD_RETURN);
        TelemetryEntity e = TelemetryEntity.fromPayload(LMashPayload.fromBytes(p.toBytes()));
        assertEquals(430572065L, e.deviceSerial);
        assertEquals(102L, e.sequence);
    }
}
