package com.example.mobile_embedded_system.transport;

import android.util.Log;

import com.example.mobile_embedded_system.data.TelemetryRepository;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.data.model.LMashPayload;
import com.example.mobile_embedded_system.data.model.PacketDiagnosticsModel;
import com.example.mobile_embedded_system.domain.TacticalCommand;
import com.example.mobile_embedded_system.security.CryptoException;
import com.example.mobile_embedded_system.security.KeyStoreManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Менеджер прямого Wi-Fi подключения к Edge-терминалу без внешнего брокера
 * (ТЗ §12, US-11, AC-07).
 * Обеспечивает прямое чтение телеметрии, отправку разрешенных команд (CHECK_IN, HOLD, RETURN)
 * и синхронизацию настроек при недоступности центрального MQTT-брокера.
 */
public class DirectConnectionManager {

    private static final String TAG = "DirectConnection";
    public static final String DEFAULT_EDGE_HOST = "192.168.4.1";
    public static final int DEFAULT_EDGE_PORT = 9000;
    public static final byte FLAG_ENCRYPTED = 0x01;
    private static final int DEDUP_CACHE_SIZE = 500;

    private String networkRoot = "mesh-a";
    private final TelemetryRepository repository;
    private final KeyStoreManager keyStoreManager;
    private final PacketDiagnosticsModel diagnosticsModel;
    private final AtomicLong commandSequence = new AtomicLong(1L);

    private final Set<String> dedupCache = Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>(DEDUP_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > DEDUP_CACHE_SIZE;
                }
            }
    );

    private Socket socket;
    private DataInputStream inStream;
    private DataOutputStream outStream;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
    }

    public DirectConnectionManager(TelemetryRepository repository, KeyStoreManager keyStoreManager, PacketDiagnosticsModel diagnosticsModel) {
        this.repository = repository;
        this.keyStoreManager = (keyStoreManager != null) ? keyStoreManager : new KeyStoreManager();
        this.diagnosticsModel = (diagnosticsModel != null) ? diagnosticsModel : new PacketDiagnosticsModel();
    }

    public void setNetworkRoot(String root) {
        this.networkRoot = root;
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && isRunning.get();
    }

    /**
     * Асинхронное прямое подключение к Wi-Fi AP Edge-устройства.
     */
    public synchronized void connect(String host, int port, ConnectionCallback callback) {
        if (isConnected()) {
            if (callback != null) callback.onConnected();
            return;
        }

        disconnect();
        isRunning.set(true);

        networkExecutor.execute(() -> {
            try {
                String targetHost = (host != null && !host.trim().isEmpty()) ? host : DEFAULT_EDGE_HOST;
                int targetPort = (port > 0) ? port : DEFAULT_EDGE_PORT;

                Log.i(TAG, "Установка прямого Wi-Fi соединения с Edge [" + targetHost + ":" + targetPort + "]...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(targetHost, targetPort), 5000);
                socket.setSoTimeout(0); // Бесконечный тайм-аут чтения входящего потока

                inStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                outStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                diagnosticsModel.setConnectionState("DIRECT_WIFI");
                diagnosticsModel.setLastReconnectTimestampMs(System.currentTimeMillis());
                diagnosticsModel.setBrokerUrl("direct://" + targetHost + ":" + targetPort);

                Log.i(TAG, "Прямое Wi-Fi соединение успешно установлено");
                if (callback != null) {
                    callback.onConnected();
                }

                startReadLoop(callback);

            } catch (IOException e) {
                Log.e(TAG, "Ошибка прямого Wi-Fi подключения к Edge: " + e.getMessage());
                disconnect();
                if (callback != null) {
                    callback.onError("Сбой прямого Wi-Fi подключения: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Цикл непрерывного чтения бинарных кадров из сокета.
     */
    private void startReadLoop(ConnectionCallback callback) {
        byte[] buffer = new byte[1024];

        while (isRunning.get() && socket != null && !socket.isClosed()) {
            try {
                // Поддержка потокового чтения LMashPayload (54 байта) или кадров с длиной
                int bytesRead = inStream.read(buffer);
                if (bytesRead < 0) {
                    Log.w(TAG, "Прямое соединение закрыто удаленным Edge-устройством");
                    break;
                }

                if (bytesRead >= LMashPayload.PAYLOAD_SIZE) {
                    byte[] packetData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, packetData, 0, bytesRead);
                    processDirectIncomingPacket(packetData);
                }
            } catch (IOException e) {
                if (isRunning.get()) {
                    Log.w(TAG, "Разрыв прямого Wi-Fi соединения: " + e.getMessage());
                }
                break;
            }
        }

        disconnect();
        if (callback != null) {
            callback.onDisconnected("Прямое Wi-Fi соединение разорвано");
        }
    }

    /**
     * Обработка принятого напрямую пакета с дедупликацией и валидацией привязок (AC-01.3, AC-07).
     */
    public void processDirectIncomingPacket(byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length < LMashPayload.PAYLOAD_SIZE) {
            diagnosticsModel.incrementMalformedPackets();
            return;
        }

        diagnosticsModel.incrementTotalPackets();
        diagnosticsModel.incrementDirectWifiPackets();

        byte[] unencryptedBytes = payloadBytes;

        // Распаковка конверта шифрования
        if (payloadBytes.length > LMashPayload.PAYLOAD_SIZE && (payloadBytes[0] & FLAG_ENCRYPTED) != 0) {
            try {
                ByteBuffer bb = ByteBuffer.wrap(payloadBytes, 1, 8);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                long keyId = bb.getLong();

                byte[] encryptedBody = new byte[payloadBytes.length - 9];
                System.arraycopy(payloadBytes, 9, encryptedBody, 0, encryptedBody.length);

                unencryptedBytes = keyStoreManager.decrypt(keyId, encryptedBody);
                Log.d(TAG, "Прямой пакет успешно расшифрован по keyId=" + keyId);
            } catch (CryptoException ce) {
                diagnosticsModel.incrementDecryptionErrors();
                Log.e(TAG, "Ошибка расшифрования прямого пакета: " + ce.getMessage());
                return;
            } catch (Exception e) {
                diagnosticsModel.incrementMalformedPackets();
                Log.e(TAG, "Ошибка разбора конверта шифрования прямого пакета", e);
                return;
            }
        }

        try {
            LMashPayload payload = LMashPayload.fromBytes(unencryptedBytes);

            if (payload.getTtl() <= 0) {
                Log.d(TAG, "Отброшен прямой пакет с TTL=" + payload.getTtl());
                return;
            }

            // In-memory дедупликация (AC-07.3, Архитектура v3 §13)
            String packetKey = payload.getDeviceSerial() + ":" + payload.getSequence();
            synchronized (dedupCache) {
                if (dedupCache.contains(packetKey)) {
                    diagnosticsModel.incrementDuplicatesDropped();
                    Log.d(TAG, "Отброшен дубликат прямого пакета: " + packetKey);
                    return;
                }
                dedupCache.add(packetKey);
            }

            // AC-01.3: Проверка несогласованности привязки
            if (!repository.isDeviceBindingValid(networkRoot, payload.getDeviceSerial(), payload.getUserId())) {
                diagnosticsModel.incrementBindingMismatchErrors();
                Log.e(TAG, "ОШИБКА НЕСОГЛАСОВАННОСТИ (AC-01.3): Прямой пакет от [" + payload.getDeviceSerial() +
                        "] с неверным userId=" + payload.getUserId());
                return;
            }

            if (payload.isTelemetry()) {
                diagnosticsModel.incrementTelemetryPackets();
            } else if (payload.isCommand()) {
                diagnosticsModel.incrementCommandPackets();
                return;
            }

            TelemetryEntity entity = TelemetryEntity.fromPayload(payload);
            entity.networkId = this.networkRoot;

            repository.insert(entity, inserted -> {
                if (!inserted) {
                    diagnosticsModel.incrementDuplicatesDropped();
                    Log.d(TAG, "Отброшен дубликат прямого пакета в БД: " + entity.deviceSerial + ":" + entity.sequence);
                } else {
                    Log.d(TAG, "Прямая телеметрия сохранена от бойца [" + entity.userId + "], seq=" + entity.sequence);
                }
            });

        } catch (IllegalArgumentException e) {
            diagnosticsModel.incrementMalformedPackets();
            Log.e(TAG, "Ошибка разбора бинарного пакета прямой телеметрии", e);
        }
    }

    /**
     * Прямая передача тактической команды на Edge-устройство через открытый сокет (US-11, AC-07.1).
     */
    public synchronized boolean sendCommandDirect(TacticalCommand command, long sourceId) {
        return sendCommandDirect(command, sourceId, 0L);
    }

    public synchronized boolean sendCommandDirect(TacticalCommand command, long sourceId, long keyId) {
        if (!isConnected()) {
            return false;
        }

        try {
            long seq = commandSequence.getAndIncrement();
            LMashPayload payload = command.toLMashPayload(seq, sourceId);
            byte[] bytes = payload.toBytes();

            if (keyId != 0L && keyStoreManager.getProfile(keyId) != null) {
                byte[] encryptedBody = keyStoreManager.encrypt(keyId, bytes);
                ByteBuffer envelope = ByteBuffer.allocate(1 + 8 + encryptedBody.length);
                envelope.order(ByteOrder.LITTLE_ENDIAN);
                envelope.put(FLAG_ENCRYPTED);
                envelope.putLong(keyId);
                envelope.put(encryptedBody);
                bytes = envelope.array();
            }

            outStream.write(bytes);
            outStream.flush();
            Log.i(TAG, "Команда успешно передана напрямую в сокет Edge (type=" + command.getCommandType() + ", seq=" + seq + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка отправки прямой команды на Edge: " + e.getMessage());
            return false;
        }
    }

    public synchronized void disconnect() {
        isRunning.set(false);
        try {
            if (inStream != null) inStream.close();
            if (outStream != null) outStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        } finally {
            inStream = null;
            outStream = null;
            socket = null;
        }
        diagnosticsModel.setConnectionState("ОТКЛЮЧЕНО");
    }
}
