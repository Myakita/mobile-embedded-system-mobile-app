package com.example.mobile_embedded_system.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.mobile_embedded_system.data.TelemetryRepository;
import com.example.mobile_embedded_system.data.local.TelemetryDao;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.data.model.LMashPayload;
import com.example.mobile_embedded_system.data.model.PacketDiagnosticsModel;
import com.example.mobile_embedded_system.domain.TacticalCommand;
import com.example.mobile_embedded_system.security.CryptoAlgorithm;
import com.example.mobile_embedded_system.security.CryptoProfile;
import com.example.mobile_embedded_system.security.KeyStoreManager;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Тестирование прямого Wi-Fi подключения к Edge-устройству (US-11, AC-07).
 */
public class DirectConnectionManagerTest {

    private DirectConnectionManager directManager;
    private MockTelemetryDao mockDao;
    private KeyStoreManager keyStoreManager;
    private PacketDiagnosticsModel diagnosticsModel;

    @Before
    public void setUp() {
        mockDao = new MockTelemetryDao();
        keyStoreManager = new KeyStoreManager();
        diagnosticsModel = new PacketDiagnosticsModel();

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

        TelemetryRepository repository = new TelemetryRepository(mockDao, directExecutor);
        directManager = new DirectConnectionManager(repository, keyStoreManager, diagnosticsModel);
    }

    @Test
    public void testProcessDirectIncomingPacketInsertsToRepository() {
        LMashPayload payload = new LMashPayload();
        payload.setMessageType(LMashPayload.MSG_TELEMETRY);
        payload.setDeviceSerial(554433L);
        payload.setSequence(10L);
        payload.setUserId(1003L);
        payload.setLatitudeE7((int) (59.9342802 * 1e7));
        payload.setLongitudeE7((int) (30.3350986 * 1e7));
        payload.setPulseBpm(85);
        payload.setTemperatureX100((short) 3660);

        byte[] packet = payload.toBytes();
        directManager.processDirectIncomingPacket(packet);

        assertEquals(1, mockDao.inserted.size());
        TelemetryEntity saved = mockDao.inserted.get(0);
        assertEquals(554433L, saved.deviceSerial);
        assertEquals(10L, saved.sequence);
        assertEquals(1003L, saved.userId);
        assertEquals(1L, diagnosticsModel.getDirectWifiPacketsCount());
    }

    @Test
    public void testDirectConnectionInMemDeduplication() {
        LMashPayload payload = new LMashPayload();
        payload.setMessageType(LMashPayload.MSG_TELEMETRY);
        payload.setDeviceSerial(554433L);
        payload.setSequence(15L);
        payload.setUserId(1003L);

        byte[] packet = payload.toBytes();
        directManager.processDirectIncomingPacket(packet);
        assertEquals(1, mockDao.inserted.size());

        // Повторная доставка того же пакета по прямому соединению (AC-07.3)
        directManager.processDirectIncomingPacket(packet);
        assertEquals(1, mockDao.inserted.size());
        assertEquals(1L, diagnosticsModel.getDuplicatesDroppedCount());
    }

    @Test
    public void testDirectIncomingPacketWithExpiredTtlDropped() {
        LMashPayload payload = new LMashPayload();
        payload.setMessageType(LMashPayload.MSG_TELEMETRY);
        payload.setDeviceSerial(554433L);
        payload.setSequence(20L);
        payload.setTtl((byte) 0);

        byte[] packet = payload.toBytes();
        directManager.processDirectIncomingPacket(packet);

        assertEquals(0, mockDao.inserted.size());
    }

    @Test
    public void testDirectIncomingEncryptedPacketDecryptedSuccessfully() throws Exception {
        long keyId = 0x55AABBCCDDEEFF00L;
        byte[] rawKey = new byte[16];
        for (int i = 0; i < 16; i++) rawKey[i] = (byte) (i + 1);

        String alias = "EDGE_AP";
        keyStoreManager.importKeyToKeyStore(alias, CryptoAlgorithm.AES_128_GCM, rawKey);
        keyStoreManager.registerProfile(new CryptoProfile("profile_ap", keyId, CryptoAlgorithm.AES_128_GCM, alias, 991122L));

        LMashPayload payload = new LMashPayload();
        payload.setMessageType(LMashPayload.MSG_TELEMETRY);
        payload.setDeviceSerial(991122L);
        payload.setSequence(55L);
        payload.setUserId(1001L);

        byte[] plainBytes = payload.toBytes();
        byte[] encryptedBody = keyStoreManager.encrypt(keyId, plainBytes);

        ByteBuffer envelope = ByteBuffer.allocate(1 + 8 + encryptedBody.length);
        envelope.order(ByteOrder.LITTLE_ENDIAN);
        envelope.put(DirectConnectionManager.FLAG_ENCRYPTED);
        envelope.putLong(keyId);
        envelope.put(encryptedBody);

        directManager.processDirectIncomingPacket(envelope.array());

        assertEquals(1, mockDao.inserted.size());
        TelemetryEntity saved = mockDao.inserted.get(0);
        assertEquals(991122L, saved.deviceSerial);
        assertEquals(55L, saved.sequence);
        assertEquals(0L, diagnosticsModel.getDecryptionErrorsCount());
    }

    private static class MockTelemetryDao implements TelemetryDao {
        final List<TelemetryEntity> inserted = new ArrayList<>();

        @Override
        public long insert(TelemetryEntity record) {
            inserted.add(record);
            return inserted.size();
        }

        @Override
        public LiveData<TelemetryEntity> getLatestTelemetryForUser(long userId) {
            return null;
        }

        @Override
        public LiveData<List<TelemetryEntity>> getHistoryForUser(long userId, long fromTimestamp) {
            return null;
        }

        @Override
        public List<TelemetryEntity> getHistoryForUserSync(long userId, long fromTimestamp) {
            return Collections.emptyList();
        }

        @Override
        public LiveData<TelemetryEntity> getLatestTelemetryForUserInNetwork(long userId, String networkId) {
            return null;
        }

        @Override
        public LiveData<List<TelemetryEntity>> getHistoryForUserInNetwork(long userId, String networkId, long fromTimestamp) {
            return null;
        }

        @Override
        public List<TelemetryEntity> getHistoryForUserInNetworkSync(long userId, String networkId, long fromTimestamp) {
            return Collections.emptyList();
        }

        @Override
        public int deleteOlderThan(long cutoffTimestampSec) {
            return 0;
        }
    }
}
