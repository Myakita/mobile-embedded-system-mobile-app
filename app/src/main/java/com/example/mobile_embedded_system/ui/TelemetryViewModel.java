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
import android.os.Handler;
import android.os.Looper;

import com.example.mobile_embedded_system.domain.GpxTrackSerializer;
import com.example.mobile_embedded_system.domain.HierarchyNode;
import com.example.mobile_embedded_system.domain.KmlTrackSerializer;
import com.example.mobile_embedded_system.security.KeyStoreManager;
import com.example.mobile_embedded_system.transport.MqttTransportManager;
import com.example.mobile_embedded_system.domain.TacticalCommand;
import com.example.mobile_embedded_system.domain.Waypoint;

import android.util.Log;
import com.example.mobile_embedded_system.data.local.CommandEntity;
import com.example.mobile_embedded_system.data.local.SubjectEntity;
import com.example.mobile_embedded_system.data.model.LMashPayload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    public enum ExportFormat {
        GPX,
        KML,
        BOTH
    }

    public interface ExportCallback {
        void onSuccess(String fileName, int count);
        void onError(String error);
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
        this.hierarchyManager = new UnitHierarchyManager(repository.getSubjectDao(), repository.getExecutorService());

        startMockMode();
    }

    public List<Long> getSquadUserIds() {
        return hierarchyManager.getAllUnitUserIds();
    }

    public String getCallsignForUser(long userId) {
        HierarchyNode node = hierarchyManager.findNodeByUserId(userId);
        return (node != null && node.getName() != null) ? node.getName().toUpperCase(Locale.ROOT) : "БОЕЦ [" + userId + "]";
    }

    public java.util.concurrent.ExecutorService getRepositoryExecutor() {
        return repository.getExecutorService();
    }

    public LiveData<String> getActiveNetworkId() {
        return activeNetworkId;
    }

    public void setActiveNetworkId(String networkId) {
        if (networkId != null && !networkId.equals(activeNetworkId.getValue())) {
            activeNetworkId.setValue(networkId);
            mqttTransport.setNetworkConfig(networkId, "7F10/21A0");
            if (repository.getSubjectDao() != null && repository.getExecutorService() != null) {
                repository.getExecutorService().execute(() -> {
                    List<SubjectEntity> subjects = repository.getSubjectDao().getSubjectsForNetworkSync(networkId);
                    hierarchyManager.switchNetwork(networkId, subjects);
                });
            }
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
     * Экспорт сессии телеметрии в форматы GPX 1.1 и/или KML 2.2 (ТЗ §4.3, §6.9, §6.10).
     */
    public void exportTrackSession(long userId, File baseDir, ExportFormat format, ExportCallback callback) {
        if (baseDir == null) {
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("НЕ УКАЗАНА ДИРЕКТОРИЯ"));
            }
            return;
        }

        repository.getExecutorService().execute(() -> {
            try {
                List<TelemetryEntity> history = repository.getHistoryForUserSync(userId, 0L);
                if (history == null || history.isEmpty()) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("НЕТ ДАННЫХ ДЛЯ ЭКСПОРТА"));
                    }
                    return;
                }

                File exportDir = new File(baseDir, "tracks");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                String callsign = getCallsignForUser(userId);
                long epochSec = System.currentTimeMillis() / 1000L;
                StringBuilder resultNames = new StringBuilder();

                if (format == ExportFormat.GPX || format == ExportFormat.BOTH) {
                    String gpxContent = GpxTrackSerializer.serialize(callsign, history);
                    String gpxFileName = String.format(Locale.US, "track_%d_%d.gpx", userId, epochSec);
                    File gpxFile = new File(exportDir, gpxFileName);
                    try (FileOutputStream fos = new FileOutputStream(gpxFile);
                         OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                        writer.write(gpxContent);
                        writer.flush();
                    }
                    resultNames.append(gpxFileName);
                }

                if (format == ExportFormat.KML || format == ExportFormat.BOTH) {
                    String kmlContent = KmlTrackSerializer.serialize(callsign, history);
                    String kmlFileName = String.format(Locale.US, "track_%d_%d.kml", userId, epochSec);
                    File kmlFile = new File(exportDir, kmlFileName);
                    try (FileOutputStream fos = new FileOutputStream(kmlFile);
                         OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                        writer.write(kmlContent);
                        writer.flush();
                    }
                    if (resultNames.length() > 0) {
                        resultNames.append(" / ");
                    }
                    resultNames.append(kmlFileName);
                }

                // Регламентная фоновая очистка записей старше 24 часов (ТЗ §4.3)
                pruneOldTelemetry(24L * 60L * 60L * 1000L);

                final String savedNames = resultNames.toString();
                final int count = history.size();
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(savedNames, count));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("ОШИБКА ЭКСПОРТА: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Передача целеуказания подчиненному юниту (автономно + по эфиру).
     * Валидация прав субординации по ТЗ (AC-02.2) и сохранение в БД (AC-05.3).
     */
    public boolean dispatchTargetCommand(long targetUserId, Waypoint waypoint) {
        if (waypoint == null) return false;

        // Валидация прав доступа (ACL) по AC-02.2
        if (!hierarchyManager.isSubordinate(activeUserId, targetUserId)) {
            Log.w("TelemetryVM", "Отказ отправки приказа: боец [" + activeUserId + "] не уполномочен командовать [" + targetUserId + "]");
            return false;
        }

        // 1. Всегда обновляем внутренний имитатор наведения
        setUnitTarget(targetUserId, waypoint.getLatitude(), waypoint.getLongitude());

        // 2. Формируем сетевой командный пакет
        TacticalCommand command = new TacticalCommand(
                targetUserId,
                waypoint.getCallsign(),
                waypoint.getLatitude(),
                waypoint.getLongitude()
        );

        // 3. Сохраняем приказ в БД (AC-05.3, DBML)
        String netId = activeNetworkId.getValue() != null ? activeNetworkId.getValue() : "mesh-a";
        repository.insertCommand(new CommandEntity(
                UUID.randomUUID().toString(),
                netId,
                activeUserId,
                targetUserId,
                command.getCommandType(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        ));

        // 4. Отправляем в эфир MQTT (если подключены)
        return mqttTransport.publishCommand(command, activeUserId);
    }

    /**
     * Передача команды запроса квитанции связи (CHECK_IN) подчиненному юниту (AC-05).
     * Валидация прав субординации по ТЗ (AC-02.2) и сохранение в БД (AC-05.3).
     */
    public boolean dispatchCheckInCommand(long targetUserId) {
        // Валидация прав доступа (ACL) по AC-02.2
        if (!hierarchyManager.isSubordinate(activeUserId, targetUserId)) {
            Log.w("TelemetryVM", "Отказ отправки CHECK_IN: боец [" + activeUserId + "] не уполномочен опрашивать [" + targetUserId + "]");
            return false;
        }

        TacticalCommand command = TacticalCommand.createCheckIn(targetUserId);

        // Сохраняем в БД (AC-05.3, DBML)
        String netId = activeNetworkId.getValue() != null ? activeNetworkId.getValue() : "mesh-a";
        repository.insertCommand(new CommandEntity(
                UUID.randomUUID().toString(),
                netId,
                activeUserId,
                targetUserId,
                LMashPayload.CMD_CHECK_IN,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        ));

        return mqttTransport.publishCommand(command, activeUserId);
    }

    public LiveData<List<CommandEntity>> getCommandsForActiveNetwork() {
        String netId = activeNetworkId.getValue() != null ? activeNetworkId.getValue() : "mesh-a";
        return repository.getCommandsForNetwork(netId);
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