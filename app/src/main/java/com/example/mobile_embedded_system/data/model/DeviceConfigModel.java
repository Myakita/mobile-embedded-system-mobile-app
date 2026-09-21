package com.example.mobile_embedded_system.data.model;

import java.util.Locale;

/**
 * Полная модель конфигурации Edge-терминала по ТЗ (§4, §11, MVP §11.50–11.59).
 */
public class DeviceConfigModel {

    // Границы периода передачи телеметрии по ТЗ §18.1
    public static final int MIN_TELEMETRY_PERIOD_SEC = 5;
    public static final int MAX_TELEMETRY_PERIOD_SEC = 300;

    public static boolean isValidTelemetryPeriod(int periodSec) {
        return periodSec >= MIN_TELEMETRY_PERIOD_SEC && periodSec <= MAX_TELEMETRY_PERIOD_SEC;
    }

    public static boolean isValidKeyLength(byte[] keyBytes) {
        return keyBytes != null && (keyBytes.length == 16 || keyBytes.length == 32);
    }

    // Идентификаторы (§11.51)
    private long deviceSerial = 99881100L;
    private long userId = 1001L;
    private long defaultDestinationId = 0L;

    // Датчики и периоды телеметрии (§11.52, §11.53)
    private boolean tempEnabled = true;
    private boolean pulseEnabled = true;
    private boolean pressureEnabled = true;
    private boolean gnssEnabled = true;
    private boolean imuEnabled = true;
    private int telemetryPeriodSec = 1;

    // Радиоинтерфейс LoRa Mesh (§11.54)
    private boolean loraEnabled = true;
    private int loraFrequencyMhz = 868;
    private int loraSpreadingFactor = 7;
    private int loraBandwidthKhz = 125;
    private int loraTxPowerDbm = 14;

    // Сетевой интерфейс Wi-Fi (§11.55)
    private boolean wifiEnabled = true;
    private String wifiSsid = "TelemetryMesh_AP";
    private String wifiPassword = "SecretPassword123";

    // Сотовый интерфейс LTE (§11.56)
    private boolean lteEnabled = true;
    private String lteApn = "internet";

    // Настройки MQTT (§11.57)
    private String brokerUrl = "tcp://broker.hivemq.com:1883";
    private String networkRoot = "mesh-a";
    private String hierarchyPath = "7F10/21A0";

    // Состояние синхронизации (§11.58, §11.59)
    private ConfigSyncState syncState = ConfigSyncState.APPLIED;
    private long lastSyncTimestampMs = System.currentTimeMillis();

    public DeviceConfigModel() {
    }

    public DeviceConfigModel(long deviceSerial, long userId, long defaultDestinationId) {
        this.deviceSerial = deviceSerial;
        this.userId = userId;
        this.defaultDestinationId = defaultDestinationId;
    }

    public long getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(long deviceSerial) { this.deviceSerial = deviceSerial; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getDefaultDestinationId() { return defaultDestinationId; }
    public void setDefaultDestinationId(long defaultDestinationId) { this.defaultDestinationId = defaultDestinationId; }

    public boolean isTempEnabled() { return tempEnabled; }
    public void setTempEnabled(boolean tempEnabled) { this.tempEnabled = tempEnabled; }

    public boolean isPulseEnabled() { return pulseEnabled; }
    public void setPulseEnabled(boolean pulseEnabled) { this.pulseEnabled = pulseEnabled; }

    public boolean isPressureEnabled() { return pressureEnabled; }
    public void setPressureEnabled(boolean pressureEnabled) { this.pressureEnabled = pressureEnabled; }

    public boolean isGnssEnabled() { return gnssEnabled; }
    public void setGnssEnabled(boolean gnssEnabled) { this.gnssEnabled = gnssEnabled; }

    public boolean isImuEnabled() { return imuEnabled; }
    public void setImuEnabled(boolean imuEnabled) { this.imuEnabled = imuEnabled; }

    public int getTelemetryPeriodSec() { return telemetryPeriodSec; }
    public void setTelemetryPeriodSec(int telemetryPeriodSec) { this.telemetryPeriodSec = telemetryPeriodSec; }

    public boolean isLoraEnabled() { return loraEnabled; }
    public void setLoraEnabled(boolean loraEnabled) { this.loraEnabled = loraEnabled; }

    public int getLoraFrequencyMhz() { return loraFrequencyMhz; }
    public void setLoraFrequencyMhz(int loraFrequencyMhz) { this.loraFrequencyMhz = loraFrequencyMhz; }

    public int getLoraSpreadingFactor() { return loraSpreadingFactor; }
    public void setLoraSpreadingFactor(int loraSpreadingFactor) { this.loraSpreadingFactor = loraSpreadingFactor; }

    public int getLoraBandwidthKhz() { return loraBandwidthKhz; }
    public void setLoraBandwidthKhz(int loraBandwidthKhz) { this.loraBandwidthKhz = loraBandwidthKhz; }

    public int getLoraTxPowerDbm() { return loraTxPowerDbm; }
    public void setLoraTxPowerDbm(int loraTxPowerDbm) { this.loraTxPowerDbm = loraTxPowerDbm; }

    public boolean isWifiEnabled() { return wifiEnabled; }
    public void setWifiEnabled(boolean wifiEnabled) { this.wifiEnabled = wifiEnabled; }

    public String getWifiSsid() { return wifiSsid; }
    public void setWifiSsid(String wifiSsid) { this.wifiSsid = wifiSsid; }

    public String getWifiPassword() { return wifiPassword; }
    public void setWifiPassword(String wifiPassword) { this.wifiPassword = wifiPassword; }

    public boolean isLteEnabled() { return lteEnabled; }
    public void setLteEnabled(boolean lteEnabled) { this.lteEnabled = lteEnabled; }

    public String getLteApn() { return lteApn; }
    public void setLteApn(String lteApn) { this.lteApn = lteApn; }

    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

    public String getNetworkRoot() { return networkRoot; }
    public void setNetworkRoot(String networkRoot) { this.networkRoot = networkRoot; }

    public String getHierarchyPath() { return hierarchyPath; }
    public void setHierarchyPath(String hierarchyPath) { this.hierarchyPath = hierarchyPath; }

    public ConfigSyncState getSyncState() { return syncState; }
    public void setSyncState(ConfigSyncState syncState) { this.syncState = syncState; }

    public long getLastSyncTimestampMs() { return lastSyncTimestampMs; }
    public void setLastSyncTimestampMs(long lastSyncTimestampMs) { this.lastSyncTimestampMs = lastSyncTimestampMs; }

    /**
     * Сериализация параметров конфигурации в JSON для передачи на Edge-терминал.
     */
    public String toJson() {
        return String.format(Locale.US,
                "{\"deviceSerial\":%d,\"userId\":%d,\"defaultDst\":%d,\"sensors\":{\"temp\":%b,\"pulse\":%b,\"press\":%b,\"gnss\":%b,\"imu\":%b,\"period\":%d},\"lora\":{\"enable\":%b,\"freq\":%d,\"sf\":%d,\"bw\":%d,\"txPwr\":%d},\"wifi\":{\"enable\":%b,\"ssid\":\"%s\"},\"lte\":{\"enable\":%b,\"apn\":\"%s\"},\"mqtt\":{\"broker\":\"%s\",\"root\":\"%s\",\"path\":\"%s\"}}",
                deviceSerial, userId, defaultDestinationId,
                tempEnabled, pulseEnabled, pressureEnabled, gnssEnabled, imuEnabled, telemetryPeriodSec,
                loraEnabled, loraFrequencyMhz, loraSpreadingFactor, loraBandwidthKhz, loraTxPowerDbm,
                wifiEnabled, escapeJson(wifiSsid),
                lteEnabled, escapeJson(lteApn),
                escapeJson(brokerUrl),
                escapeJson(networkRoot),
                escapeJson(hierarchyPath)
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format(Locale.US, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
