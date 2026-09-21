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
        this(telemetryDao, deviceConfigDao, null, null, null, null, executorService);
    }

    public TelemetryRepository(TelemetryDao telemetryDao,
                               DeviceConfigDao deviceConfigDao,
                               NetworkDao networkDao,
                               SubjectDao subjectDao,
                               DeviceDao deviceDao,
                               CommandDao commandDao,
                               ExecutorService executorService) {
        this.telemetryDao = telemetryDao;
        this.deviceConfigDao = deviceConfigDao;
        this.networkDao = networkDao;
        this.subjectDao = subjectDao;
        this.deviceDao = deviceDao;
        this.commandDao = commandDao;
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

    public LiveData<TelemetryEntity> getLatestTelemetryForUserInNetwork(long userId, String networkId) {
        return telemetryDao != null ? telemetryDao.getLatestTelemetryForUserInNetwork(userId, networkId) : null;
    }

    public LiveData<List<TelemetryEntity>> getHistoryForUserInNetwork(long userId, String networkId, long sinceTimestamp) {
        return telemetryDao != null ? telemetryDao.getHistoryForUserInNetwork(userId, networkId, sinceTimestamp) : null;
    }

    public List<TelemetryEntity> getHistoryForUserInNetworkSync(long userId, String networkId, long sinceTimestamp) {
        return telemetryDao != null ? telemetryDao.getHistoryForUserInNetworkSync(userId, networkId, sinceTimestamp) : Collections.emptyList();
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
                    DeviceEntity existing = deviceDao.getDeviceBySerialSync(device.networkId, device.serial);
                    if (existing != null) {
                        if (!java.util.Objects.equals(existing.subjectId, device.subjectId)) {
                            android.util.Log.w("TelemetryRepository", "Silent overwrite attempt blocked for device: " + device.serial);
                            if (callback != null) callback.onResult(false);
                        } else {
                            existing.lastSeenMs = device.lastSeenMs;
                            deviceDao.update(existing);
                            if (callback != null) callback.onResult(true);
                        }
                    } else {
                        deviceDao.insertWithAbort(device);
                        if (callback != null) callback.onResult(true);
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onResult(false);
                }
            });
        }
    }

    /**
     * Явное переназначение устройства новому бойцу (AC-01.3).
     */
    public void reassignDevice(String networkId, long serial, String newSubjectId, InsertCallback callback) {
        if (deviceDao != null && executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    DeviceEntity existing = deviceDao.getDeviceBySerialSync(networkId, serial);
                    if (existing != null) {
                        existing.subjectId = newSubjectId;
                        existing.lastSeenMs = System.currentTimeMillis();
                        deviceDao.update(existing);
                        if (callback != null) callback.onResult(true);
                    } else {
                        DeviceEntity dev = new DeviceEntity(
                                java.util.UUID.randomUUID().toString(),
                                networkId,
                                serial,
                                newSubjectId,
                                System.currentTimeMillis()
                        );
                        deviceDao.insertWithAbort(dev);
                        if (callback != null) callback.onResult(true);
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onResult(false);
                }
            });
        }
    }

    /**
     * Проверка соответствия серийного номера устройства и userId по таблице привязок (AC-01.3).
     * @return true если привязка совпадает либо устройство пока не привязано в БД.
     */
    public boolean isDeviceBindingValid(String networkId, long deviceSerial, long userId) {
        if (deviceDao == null || subjectDao == null) return true;
        try {
            DeviceEntity dev = deviceDao.getDeviceBySerialSync(networkId, deviceSerial);
            if (dev == null || dev.subjectId == null) return true;
            SubjectEntity subj = subjectDao.getSubjectByIdSync(dev.subjectId);
            if (subj == null || subj.userId == null) return true;
            return subj.userId.longValue() == userId;
        } catch (Exception e) {
            return true;
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

    public DeviceEntity getDeviceBySubjectSync(String networkId, String subjectId) {
        return deviceDao != null ? deviceDao.getDeviceBySubjectSync(networkId, subjectId) : null;
    }

    public DeviceEntity getDeviceForUserSync(String networkId, long userId) {
        if (subjectDao != null && deviceDao != null) {
            SubjectEntity subject = subjectDao.getSubjectByUserIdSync(networkId, userId);
            if (subject != null) {
                DeviceEntity device = deviceDao.getDeviceBySubjectSync(networkId, subject.id);
                if (device != null) return device;
            }
            DeviceEntity devDirect = deviceDao.getDeviceBySubjectSync(networkId, String.valueOf(userId));
            if (devDirect != null) return devDirect;
            return deviceDao.getDeviceBySubjectSync(networkId, "sub-" + userId);
        }
        return null;
    }
}