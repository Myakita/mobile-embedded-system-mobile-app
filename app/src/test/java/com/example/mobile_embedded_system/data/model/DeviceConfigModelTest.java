package com.example.mobile_embedded_system.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Модульное тестирование модели параметров конфигурации Edge-устройства (ТЗ §5.1, §11).
 */
public class DeviceConfigModelTest {

    @Test
    public void testDeviceConfigDefaultValues() {
        DeviceConfigModel config = new DeviceConfigModel(99881100L, 1001L, 0L);

        assertEquals(99881100L, config.getDeviceSerial());
        assertEquals(1001L, config.getUserId());
        assertEquals(0L, config.getDefaultDestinationId());
        assertTrue(config.isTempEnabled());
        assertTrue(config.isPulseEnabled());
        assertTrue(config.isPressureEnabled());
        assertTrue(config.isGnssEnabled());
        assertTrue(config.isImuEnabled());
        assertEquals(1, config.getTelemetryPeriodSec());
        assertEquals(ConfigSyncState.APPLIED, config.getSyncState());
    }

    @Test
    public void testDeviceConfigJsonSerialization() {
        DeviceConfigModel config = new DeviceConfigModel(99881122L, 1002L, 1001L);
        config.setLoraFrequencyMhz(868);
        config.setWifiSsid("TacticalAP");
        config.setLteApn("custom.apn");

        String json = config.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"deviceSerial\":99881122"));
        assertTrue(json.contains("\"userId\":1002"));
        assertTrue(json.contains("\"freq\":868"));
        assertTrue(json.contains("\"ssid\":\"TacticalAP\""));
        assertTrue(json.contains("\"apn\":\"custom.apn\""));
    }

    @Test
    public void testSyncStateTransitions() {
        DeviceConfigModel config = new DeviceConfigModel();
        config.setSyncState(ConfigSyncState.CONFIGURATION_REQUIRED);
        assertEquals("Требует обновления конфигурации", config.getSyncState().getDescription());

        config.setSyncState(ConfigSyncState.PENDING);
        assertEquals("Ожидает применения", config.getSyncState().getDescription());

        config.setSyncState(ConfigSyncState.APPLIED);
        assertEquals("Применено", config.getSyncState().getDescription());
    }

    @Test
    public void testTelemetryPeriodAndKeyValidation() {
        // ТЗ §18.1: диапазон от 5 до 300 секунд
        assertFalse(DeviceConfigModel.isValidTelemetryPeriod(0));
        assertFalse(DeviceConfigModel.isValidTelemetryPeriod(4));
        assertTrue(DeviceConfigModel.isValidTelemetryPeriod(5));
        assertTrue(DeviceConfigModel.isValidTelemetryPeriod(60));
        assertTrue(DeviceConfigModel.isValidTelemetryPeriod(300));
        assertFalse(DeviceConfigModel.isValidTelemetryPeriod(301));

        // Валидация криптографических ключей (16 или 32 байта)
        assertFalse(DeviceConfigModel.isValidKeyLength(null));
        assertFalse(DeviceConfigModel.isValidKeyLength(new byte[8]));
        assertTrue(DeviceConfigModel.isValidKeyLength(new byte[16]));
        assertTrue(DeviceConfigModel.isValidKeyLength(new byte[32]));
        assertFalse(DeviceConfigModel.isValidKeyLength(new byte[64]));
    }
}
