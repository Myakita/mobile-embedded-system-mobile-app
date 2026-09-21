package com.example.mobile_embedded_system.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.mobile_embedded_system.data.model.ConfigSyncState;
import com.example.mobile_embedded_system.data.model.DeviceConfigModel;

/**
 * Локальное хранилище конфигурации Edge-терминала в Room DB (ТЗ §11.50–11.59, §19).
 */
@Entity(tableName = "device_configs")
public class DeviceConfigEntity {

    @PrimaryKey
    @ColumnInfo(name = "device_serial")
    public long deviceSerial;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "default_destination_id")
    public long defaultDestinationId;

    @ColumnInfo(name = "temp_enabled")
    public boolean tempEnabled;

    @ColumnInfo(name = "pulse_enabled")
    public boolean pulseEnabled;

    @ColumnInfo(name = "pressure_enabled")
    public boolean pressureEnabled;

    @ColumnInfo(name = "gnss_enabled")
    public boolean gnssEnabled;

    @ColumnInfo(name = "imu_enabled")
    public boolean imuEnabled;

    @ColumnInfo(name = "telemetry_period_sec")
    public int telemetryPeriodSec;

    @ColumnInfo(name = "lora_enabled")
    public boolean loraEnabled;

    @ColumnInfo(name = "lora_frequency_mhz")
    public int loraFrequencyMhz;

    @ColumnInfo(name = "lora_spreading_factor")
    public int loraSpreadingFactor;

    @ColumnInfo(name = "lora_bandwidth_khz")
    public int loraBandwidthKhz;

    @ColumnInfo(name = "lora_tx_power_dbm")
    public int loraTxPowerDbm;

    @ColumnInfo(name = "wifi_enabled")
    public boolean wifiEnabled;

    @ColumnInfo(name = "wifi_ssid")
    public String wifiSsid;

    @ColumnInfo(name = "wifi_password")
    public String wifiPassword;

    @ColumnInfo(name = "lte_enabled")
    public boolean lteEnabled;

    @ColumnInfo(name = "lte_apn")
    public String lteApn;

    @ColumnInfo(name = "broker_url")
    public String brokerUrl;

    @ColumnInfo(name = "network_root")
    public String networkRoot;

    @ColumnInfo(name = "hierarchy_path")
    public String hierarchyPath;

    @ColumnInfo(name = "sync_state")
    public String syncState;

    @ColumnInfo(name = "last_sync_timestamp_ms")
    public long lastSyncTimestampMs;

    public DeviceConfigEntity() {
    }

    public DeviceConfigModel toModel() {
        DeviceConfigModel model = new DeviceConfigModel();
        model.setDeviceSerial(deviceSerial);
        model.setUserId(userId);
        model.setDefaultDestinationId(defaultDestinationId);
        model.setTempEnabled(tempEnabled);
        model.setPulseEnabled(pulseEnabled);
        model.setPressureEnabled(pressureEnabled);
        model.setGnssEnabled(gnssEnabled);
        model.setImuEnabled(imuEnabled);
        model.setTelemetryPeriodSec(telemetryPeriodSec);
        model.setLoraEnabled(loraEnabled);
        model.setLoraFrequencyMhz(loraFrequencyMhz);
        model.setLoraSpreadingFactor(loraSpreadingFactor);
        model.setLoraBandwidthKhz(loraBandwidthKhz);
        model.setLoraTxPowerDbm(loraTxPowerDbm);
        model.setWifiEnabled(wifiEnabled);
        model.setWifiSsid(wifiSsid);
        model.setWifiPassword(wifiPassword);
        model.setLteEnabled(lteEnabled);
        model.setLteApn(lteApn);
        model.setBrokerUrl(brokerUrl);
        model.setNetworkRoot(networkRoot);
        model.setHierarchyPath(hierarchyPath);
        try {
            model.setSyncState(ConfigSyncState.valueOf(syncState));
        } catch (Exception e) {
            model.setSyncState(ConfigSyncState.APPLIED);
        }
        model.setLastSyncTimestampMs(lastSyncTimestampMs);
        return model;
    }

    public static DeviceConfigEntity fromModel(@NonNull DeviceConfigModel model) {
        DeviceConfigEntity entity = new DeviceConfigEntity();
        entity.deviceSerial = model.getDeviceSerial();
        entity.userId = model.getUserId();
        entity.defaultDestinationId = model.getDefaultDestinationId();
        entity.tempEnabled = model.isTempEnabled();
        entity.pulseEnabled = model.isPulseEnabled();
        entity.pressureEnabled = model.isPressureEnabled();
        entity.gnssEnabled = model.isGnssEnabled();
        entity.imuEnabled = model.isImuEnabled();
        entity.telemetryPeriodSec = model.getTelemetryPeriodSec();
        entity.loraEnabled = model.isLoraEnabled();
        entity.loraFrequencyMhz = model.getLoraFrequencyMhz();
        entity.loraSpreadingFactor = model.getLoraSpreadingFactor();
        entity.loraBandwidthKhz = model.getLoraBandwidthKhz();
        entity.loraTxPowerDbm = model.getLoraTxPowerDbm();
        entity.wifiEnabled = model.isWifiEnabled();
        entity.wifiSsid = model.getWifiSsid();
        entity.wifiPassword = model.getWifiPassword();
        entity.lteEnabled = model.isLteEnabled();
        entity.lteApn = model.getLteApn();
        entity.brokerUrl = model.getBrokerUrl();
        entity.networkRoot = model.getNetworkRoot();
        entity.hierarchyPath = model.getHierarchyPath();
        entity.syncState = model.getSyncState() != null ? model.getSyncState().name() : ConfigSyncState.APPLIED.name();
        entity.lastSyncTimestampMs = model.getLastSyncTimestampMs();
        return entity;
    }
}
