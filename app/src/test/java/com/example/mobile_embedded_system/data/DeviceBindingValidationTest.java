package com.example.mobile_embedded_system.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.mobile_embedded_system.data.local.DeviceDao;
import com.example.mobile_embedded_system.data.local.DeviceEntity;
import com.example.mobile_embedded_system.data.local.SubjectDao;
import com.example.mobile_embedded_system.data.local.SubjectEntity;
import com.example.mobile_embedded_system.data.local.TelemetryDao;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Тестирование критериев AC-01.2 и AC-01.3 (проверка привязок устройств и детектирование несогласованности).
 */
public class DeviceBindingValidationTest {

    private TelemetryRepository repository;
    private MockDeviceDao mockDeviceDao;
    private MockSubjectDao mockSubjectDao;

    @Before
    public void setUp() {
        mockDeviceDao = new MockDeviceDao();
        mockSubjectDao = new MockSubjectDao();

        ExecutorService directExecutor = new AbstractExecutorService() {
            @Override
            public void shutdown() {}
            @NonNull
            @Override
            public List<Runnable> shutdownNow() { return Collections.emptyList(); }
            @Override
            public boolean isShutdown() { return false; }
            @Override
            public boolean isTerminated() { return false; }
            @Override
            public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) { return true; }
            @Override
            public void execute(@NonNull Runnable command) { command.run(); }
        };

        repository = new TelemetryRepository(
                new MockTelemetryDao(),
                null,
                null,
                mockSubjectDao,
                mockDeviceDao,
                null,
                directExecutor
        );
    }

    @Test
    public void testDuplicateDeviceRegistrationToDifferentUserBlocked() {
        // AC-01.2: Уже привязанное устройство нельзя незаметно назначить второму пользователю.
        DeviceEntity device1 = new DeviceEntity("dev-1", "mesh-a", 99881100L, "subj-1001", System.currentTimeMillis());

        AtomicBoolean result1 = new AtomicBoolean(false);
        repository.registerDevice(device1, result1::set);
        assertTrue(result1.get());

        // Попытка тихой регистрации того же серийного номера другому пользователю "subj-1002"
        DeviceEntity device2 = new DeviceEntity("dev-2", "mesh-a", 99881100L, "subj-1002", System.currentTimeMillis());
        AtomicBoolean result2 = new AtomicBoolean(true);
        repository.registerDevice(device2, result2::set);
        assertFalse("Повторная регистрация на другого пользователя должна отклоняться (AC-01.2)", result2.get());

        // Привязка осталась за исходным пользователем
        DeviceEntity existing = repository.getDeviceBySerialSync("mesh-a", 99881100L);
        assertEquals("subj-1001", existing.subjectId);
    }

    @Test
    public void testExplicitDeviceReassignmentAllowed() {
        // AC-01.3: После явного переназначения устройства новые сообщения относятся к новому пользователю
        DeviceEntity device1 = new DeviceEntity("dev-1", "mesh-a", 99881100L, "subj-1001", System.currentTimeMillis());
        repository.registerDevice(device1, null);

        AtomicBoolean reassignResult = new AtomicBoolean(false);
        repository.reassignDevice("mesh-a", 99881100L, "subj-1002", reassignResult::set);
        assertTrue(reassignResult.get());

        DeviceEntity updated = repository.getDeviceBySerialSync("mesh-a", 99881100L);
        assertEquals("subj-1002", updated.subjectId);
    }

    @Test
    public void testDeviceBindingValidationMatchesSubjectUserId() {
        // AC-01.3: Проверка соответствия привязки
        SubjectEntity subject1 = new SubjectEntity("subj-1001", "mesh-a", 1001L, "КОМАНДИР", null);
        mockSubjectDao.insert(subject1);

        DeviceEntity device1 = new DeviceEntity("dev-1", "mesh-a", 99881100L, "subj-1001", System.currentTimeMillis());
        repository.registerDevice(device1, null);

        // Пакет с deviceSerial=99881100 и userId=1001 - валиден
        assertTrue(repository.isDeviceBindingValid("mesh-a", 99881100L, 1001L));

        // Пакет с тем же deviceSerial, но чужим userId=1002 - невалиден (несогласованность)
        assertFalse(repository.isDeviceBindingValid("mesh-a", 99881100L, 1002L));
    }

    @Test
    public void testGetDeviceForUserSync() {
        SubjectEntity subject1 = new SubjectEntity("subj-1001", "mesh-a", 1001L, "КОМАНДИР", null);
        mockSubjectDao.insert(subject1);

        DeviceEntity device1 = new DeviceEntity("dev-1", "mesh-a", 99881100L, "subj-1001", System.currentTimeMillis());
        repository.registerDevice(device1, null);

        DeviceEntity found = repository.getDeviceForUserSync("mesh-a", 1001L);
        org.junit.Assert.assertNotNull(found);
        assertEquals(99881100L, found.serial);
    }

    private static class MockDeviceDao implements DeviceDao {
        private final Map<String, DeviceEntity> devices = new HashMap<>();

        @Override
        public void insert(DeviceEntity device) {
            devices.put(device.networkId + ":" + device.serial, device);
        }

        @Override
        public void update(DeviceEntity device) {
            devices.put(device.networkId + ":" + device.serial, device);
        }

        @Override
        public long insertWithAbort(DeviceEntity device) {
            String key = device.networkId + ":" + device.serial;
            if (devices.containsKey(key)) {
                throw new RuntimeException("UNIQUE constraint failed");
            }
            devices.put(key, device);
            return 1L;
        }

        @Override
        public LiveData<List<DeviceEntity>> getDevicesForNetwork(String networkId) { return null; }

        @Override
        public List<DeviceEntity> getDevicesForNetworkSync(String networkId) {
            List<DeviceEntity> list = new ArrayList<>();
            for (DeviceEntity d : devices.values()) {
                if (d.networkId.equals(networkId)) list.add(d);
            }
            return list;
        }

        @Override
        public DeviceEntity getDeviceBySerialSync(String networkId, long serial) {
            return devices.get(networkId + ":" + serial);
        }

        @Override
        public DeviceEntity getDeviceBySubjectSync(String networkId, String subjectId) {
            for (DeviceEntity d : devices.values()) {
                if (d.networkId.equals(networkId) && subjectId.equals(d.subjectId)) return d;
            }
            return null;
        }

        @Override
        public void deleteById(String id) {}
    }

    private static class MockSubjectDao implements SubjectDao {
        private final Map<String, SubjectEntity> subjects = new HashMap<>();

        @Override
        public void insert(SubjectEntity subject) {
            subjects.put(subject.id, subject);
        }

        @Override
        public void insertAll(List<SubjectEntity> list) {
            for (SubjectEntity s : list) subjects.put(s.id, s);
        }

        @Override
        public List<SubjectEntity> getAllSubjectsSync() { return new ArrayList<>(subjects.values()); }

        @Override
        public void deleteById(String id) { subjects.remove(id); }

        @Override
        public void deleteAll() { subjects.clear(); }

        @Override
        public LiveData<List<SubjectEntity>> getSubjectsForNetwork(String networkId) { return null; }

        @Override
        public List<SubjectEntity> getSubjectsForNetworkSync(String networkId) { return new ArrayList<>(subjects.values()); }

        @Override
        public SubjectEntity getSubjectByIdSync(String id) {
            return subjects.get(id);
        }

        @Override
        public SubjectEntity getSubjectByUserIdSync(String networkId, long userId) {
            for (SubjectEntity s : subjects.values()) {
                if (s.networkId.equals(networkId) && s.userId != null && s.userId == userId) return s;
            }
            return null;
        }

        @Override
        public void deleteForNetwork(String networkId) {}
    }

    private static class MockTelemetryDao implements TelemetryDao {
        @Override
        public long insert(TelemetryEntity record) { return 1L; }
        @Override
        public LiveData<TelemetryEntity> getLatestTelemetryForUser(long userId) { return null; }
        @Override
        public LiveData<List<TelemetryEntity>> getHistoryForUser(long userId, long fromTimestamp) { return null; }
        @Override
        public List<TelemetryEntity> getHistoryForUserSync(long userId, long fromTimestamp) { return Collections.emptyList(); }
        @Override
        public LiveData<TelemetryEntity> getLatestTelemetryForUserInNetwork(long userId, String networkId) { return null; }
        @Override
        public LiveData<List<TelemetryEntity>> getHistoryForUserInNetwork(long userId, String networkId, long fromTimestamp) { return null; }
        @Override
        public List<TelemetryEntity> getHistoryForUserInNetworkSync(long userId, String networkId, long fromTimestamp) { return Collections.emptyList(); }
        @Override
        public int deleteOlderThan(long cutoffTimestampSec) { return 0; }
    }
}
