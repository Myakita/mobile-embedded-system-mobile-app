package com.example.mobile_embedded_system.data;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.mobile_embedded_system.data.local.AppDatabase;
import com.example.mobile_embedded_system.data.local.CommandDao;
import com.example.mobile_embedded_system.data.local.CommandEntity;
import com.example.mobile_embedded_system.data.local.DeviceConfigDao;
import com.example.mobile_embedded_system.data.local.DeviceConfigEntity;
import com.example.mobile_embedded_system.data.local.DeviceDao;
import com.example.mobile_embedded_system.data.local.DeviceEntity;
import com.example.mobile_embedded_system.data.local.NetworkDao;
import com.example.mobile_embedded_system.data.local.NetworkEntity;
import com.example.mobile_embedded_system.data.local.SubjectDao;
import com.example.mobile_embedded_system.data.local.SubjectEntity;
import com.example.mobile_embedded_system.data.local.TelemetryDao;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Репозиторий слоя данных телеметрии (ТЗ §4.2, Архитектура MVVM).
 */
public class TelemetryRepository {

    private final TelemetryDao telemetryDao;
    private final DeviceConfigDao deviceConfigDao;
    private final NetworkDao networkDao;
    private final SubjectDao subjectDao;
    private final DeviceDao deviceDao;
    private final CommandDao commandDao;
    private final ExecutorService executorService;

    public TelemetryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.telemetryDao = db.telemetryDao();
        this.deviceConfigDao = db.deviceConfigDao();
        this.networkDao = db.networkDao();
        this.subjectDao = db.subjectDao();
        this.deviceDao = db.deviceDao();
        this.commandDao = db.commandDao();
        this.executorService = Executors.newSingleThreadExecutor();

        initDefaultNetwork();
    }

    public TelemetryRepository(TelemetryDao telemetryDao, ExecutorService executorService) {
        this(telemetryDao, null, executorService);
    }

    public TelemetryRepository(TelemetryDao telemetryDao, DeviceConfigDao deviceConfigDao, ExecutorService executorService) {
        this.telemetryDao = telemetryDao;
        this.deviceConfigDao = deviceConfigDao;
        this.networkDao = null;
        this.subjectDao = null;
        this.deviceDao = null;
        this.commandDao = null;
        this.executorService = executorService;
    }

    private void initDefaultNetwork() {
        executorService.execute(() -> {
            if (networkDao != null && networkDao.getNetworkSync("mesh-a") == null) {
                networkDao.insert(new NetworkEntity("mesh-a", "Мультисеть А", "mesh-a", System.currentTimeMillis()));
                networkDao.insert(new NetworkEntity("mesh-b", "Мультисеть Б", "mesh-b", System.currentTimeMillis()));
            }
        });
    }

    public LiveData<List<NetworkEntity>> getAllNetworks() {
        return networkDao != null ? networkDao.getAllNetworks() : null;
    }

    public void createNetwork(NetworkEntity network) {
        if (networkDao != null && network != null) {
            executorService.execute(() -> networkDao.insert(network));
        }
    }

    public interface InsertCallback {
        void onResult(boolean inserted);
    }

    public void insert(TelemetryEntity entity) {
        insert(entity, null);
    }

    public void insert(TelemetryEntity entity, InsertCallback callback) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                long rowId = telemetryDao.insert(entity);
                if (callback != null) {
                    callback.onResult(rowId != -1L);
                }
            });
        }
    }

    public void saveDeviceConfig(DeviceConfigEntity config) {
        if (deviceConfigDao != null && executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> deviceConfigDao.insertOrUpdate(config));
        }
    }

    public LiveData<DeviceConfigEntity> getDeviceConfig(long deviceSerial) {
        return deviceConfigDao != null ? deviceConfigDao.getConfigForDevice(deviceSerial) : null;
    }

    public LiveData<TelemetryEntity> getLatestTelemetryForUser(long userId) {
        return telemetryDao.getLatestTelemetryForUser(userId);
    }

    public LiveData<List<TelemetryEntity>> getHistoryForUser(long userId, long sinceTimestamp) {
        return telemetryDao.getHistoryForUser(userId, sinceTimestamp);
    }

    public List<TelemetryEntity> getHistoryForUserSync(long userId, long sinceTimestamp) {
        return telemetryDao != null ? telemetryDao.getHistoryForUserSync(userId, sinceTimestamp) : Collections.emptyList();
    }

    /**
     * Фоновая регламентная очистка старых пакетов телеметрии через управляемый пул потоков.
     * @param cutoffTimestampSec временная граница в секундах Unix Epoch.
     */
    public void pruneOlderThan(long cutoffTimestampSec) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    telemetryDao.deleteOlderThan(cutoffTimestampSec);
                } catch (Exception ignored) {
                }
            });
        }
    }

    public SubjectDao getSubjectDao() {
        return subjectDao;
    }

    public CommandDao getCommandDao() {
        return commandDao;
    }

    public DeviceDao getDeviceDao() {
        return deviceDao;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void insertCommand(CommandEntity command) {
        if (commandDao != null && executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> commandDao.insert(command));
        }
    }

    public LiveData<List<CommandEntity>> getCommandsForNetwork(String networkId) {
        return commandDao != null ? commandDao.getCommandsForNetwork(networkId) : null;
    }

    public List<CommandEntity> getCommandsForNetworkSync(String networkId) {
        return commandDao != null ? commandDao.getCommandsForNetworkSync(networkId) : Collections.emptyList();
    }

    public void registerDevice(DeviceEntity device, InsertCallback callback) {
        if (deviceDao != null && executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    deviceDao.insert(device);
                    if (callback != null) callback.onResult(true);
                } catch (Exception e) {
                    if (callback != null) callback.onResult(false);
                }
            });
        }
    }

    public LiveData<List<DeviceEntity>> getDevicesForNetwork(String networkId) {
        return deviceDao != null ? deviceDao.getDevicesForNetwork(networkId) : null;
    }

    public List<DeviceEntity> getDevicesForNetworkSync(String networkId) {
        return deviceDao != null ? deviceDao.getDevicesForNetworkSync(networkId) : Collections.emptyList();
    }

    public DeviceEntity getDeviceBySerialSync(String networkId, long serial) {
        return deviceDao != null ? deviceDao.getDeviceBySerialSync(networkId, serial) : null;
    }
}