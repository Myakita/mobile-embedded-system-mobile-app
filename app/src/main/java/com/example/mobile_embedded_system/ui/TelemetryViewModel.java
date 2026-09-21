package com.example.mobile_embedded_system.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile_embedded_system.data.MockTelemetryGenerator;
import com.example.mobile_embedded_system.data.TelemetryRepository;
import com.example.mobile_embedded_system.data.local.DeviceConfigEntity;
import com.example.mobile_embedded_system.data.local.NetworkEntity;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.data.model.ConfigSyncState;
import com.example.mobile_embedded_system.data.model.DeviceConfigModel;
import com.example.mobile_embedded_system.data.model.PacketDiagnosticsModel;
import com.example.mobile_embedded_system.domain.TacticalWaypointManager;
import com.example.mobile_embedded_system.domain.UnitHierarchyManager;
import com.example.mobile_embedded_system.security.KeyStoreManager;
import com.example.mobile_embedded_system.transport.MqttTransportManager;
import com.example.mobile_embedded_system.domain.TacticalCommand;
import com.example.mobile_embedded_system.domain.Waypoint;

import java.util.List;

/**
 * MVVM-фасад для доступа к телеметрии и сохранения состояния экрана (ТЗ §4.2).
 */
public class TelemetryViewModel extends AndroidViewModel {

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        MOCK_MODE
    }

    private final TelemetryRepository repository;
    private final KeyStoreManager keyStoreManager;
    private final MockTelemetryGenerator mockGenerator;
    private final MqttTransportManager mqttTransport;
    private final TacticalWaypointManager waypointManager;
    private final UnitHierarchyManager hierarchyManager;

    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<String> activeNetworkId = new MutableLiveData<>("mesh-a");

    // Сохранение состояния экрана при смене тем и конфигурации
    private double lastLat = Double.NaN;
    private double lastLon = Double.NaN;
    private double lastZoom = 14.0;
    private double lastBearing = 0.0;
    private boolean hasSavedCamera = false;

    private long activeUserId = 1001L;
    private boolean isTrackUp = false;

    public TelemetryViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TelemetryRepository(application);
        this.keyStoreManager = new KeyStoreManager();
        this.mockGenerator = new MockTelemetryGenerator(this);
        this.mqttTransport = new MqttTransportManager(repository, keyStoreManager);
        this.waypointManager = new TacticalWaypointManager();
        this.hierarchyManager = new UnitHierarchyManager();

        startMockMode();
    }

    public LiveData<String> getActiveNetworkId() {
        return activeNetworkId;
    }

    public void setActiveNetworkId(String networkId) {
        if (networkId != null && !networkId.equals(activeNetworkId.getValue())) {
            activeNetworkId.setValue(networkId);
            mqttTransport.setNetworkConfig(networkId, "7F10/21A0");
        }
    }

    public LiveData<List<NetworkEntity>> getAllNetworks() {
        return repository.getAllNetworks();
    }

    public void createNetwork(String id, String name, String mqttRoot) {
        repository.createNetwork(new NetworkEntity(id, name, mqttRoot, System.currentTimeMillis()));
    }

    public KeyStoreManager getKeyStoreManager() {
        return keyStoreManager;
    }

    public TacticalWaypointManager getWaypointManager() {
        return waypointManager;
    }

    public UnitHierarchyManager getHierarchyManager() {
        return hierarchyManager;
    }

    public void saveCameraState(double lat, double lon, double zoom, double bearing) {
        this.lastLat = lat;
        this.lastLon = lon;
        this.lastZoom = zoom;
        this.lastBearing = bearing;
        this.hasSavedCamera = true;
    }

    public boolean hasSavedCamera() {
        return hasSavedCamera;
    }

    public double getLastLat() { return lastLat; }
    public double getLastLon() { return lastLon; }
    public double getLastZoom() { return lastZoom; }
    public double getLastBearing() { return lastBearing; }

    public long getActiveUserId() {
        return activeUserId;
    }

    public void setActiveUserId(long activeUserId) {
        this.activeUserId = activeUserId;
    }

    public boolean isTrackUp() {
        return isTrackUp;
    }

    public void setTrackUp(boolean trackUp) {
        isTrackUp = trackUp;
    }

    public void insertTelemetry(TelemetryEntity entity) {
        repository.insert(entity);
    }

    public LiveData<TelemetryEntity> getLatestTelemetry(long userId) {
        return repository.getLatestTelemetryForUser(userId);
    }

    public LiveData<List<TelemetryEntity>> getHistory(long userId, long since) {
        return repository.getHistoryForUser(userId, since);
    }

    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    public PacketDiagnosticsModel getDiagnosticsModel() {
        return mqttTransport.getDiagnosticsModel();
    }

    public void setUnitTarget(long userId, double targetLat, double targetLon) {
        mockGenerator.setUnitTarget(userId, targetLat, targetLon);
    }

    public void clearUnitTarget(long userId) {
        mockGenerator.clearUnitTarget(userId);
    }

    public void startMockMode() {
        mqttTransport.disconnect();
        mockGenerator.start();
        connectionState.postValue(ConnectionState.MOCK_MODE);
    }

    public void startMqttMode(String brokerUrl, String clientId) {
        mockGenerator.stop();
        connectionState.postValue(ConnectionState.CONNECTING);

        mqttTransport.connect(brokerUrl, clientId, new MqttTransportManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                connectionState.postValue(ConnectionState.CONNECTED);
            }

            @Override
            public void onDisconnected(String reason) {
                connectionState.postValue(ConnectionState.DISCONNECTED);
            }

            @Override
            public void onError(String error) {
                connectionState.postValue(ConnectionState.DISCONNECTED);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mockGenerator.stop();
        mqttTransport.disconnect();
    }
    public void pruneOldTelemetry(long retentionMillis) {
        long cutoffTimestampSec = (System.currentTimeMillis() - retentionMillis) / 1000L;
        repository.pruneOlderThan(cutoffTimestampSec);
    }

    /**
     * Передача целеуказания подчиненному юниту (автономно + по эфиру).
     */
    public boolean dispatchTargetCommand(long targetUserId, Waypoint waypoint) {
        // 1. Всегда обновляем внутренний имитатор наведения
        setUnitTarget(targetUserId, waypoint.getLatitude(), waypoint.getLongitude());

        // 2. Формируем сетевой командный пакет
        TacticalCommand command = new TacticalCommand(
                targetUserId,
                waypoint.getCallsign(),
                waypoint.getLatitude(),
                waypoint.getLongitude()
        );

        // 3. Отправляем в эфир MQTT (если подключены)
        return mqttTransport.publishCommand(command, activeUserId);
    }

    public LiveData<DeviceConfigEntity> getDeviceConfig(long deviceSerial) {
        return repository.getDeviceConfig(deviceSerial);
    }

    /**
     * Сохранение конфигурации устройства.
     * По ТЗ §18 и AC-03 статус выставляется PENDING («Ожидает применения»).
     * Локальное сохранение не считается успешным применением на устройстве (APPLIED).
     */
    public boolean saveAndTransmitDeviceConfig(DeviceConfigModel config) {
        if (config == null) return false;

        config.setSyncState(ConfigSyncState.PENDING);
        config.setLastSyncTimestampMs(System.currentTimeMillis());

        repository.saveDeviceConfig(DeviceConfigEntity.fromModel(config));
        return true;
    }
}