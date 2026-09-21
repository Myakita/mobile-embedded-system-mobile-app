package com.example.mobile_embedded_system.transport;

import android.util.Log;

import com.example.mobile_embedded_system.data.TelemetryRepository;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.data.model.DeviceConfigModel;
import com.example.mobile_embedded_system.data.model.LMashPayload;
import com.example.mobile_embedded_system.data.model.PacketDiagnosticsModel;
import com.example.mobile_embedded_system.domain.network.MqttTopicBuilder;
import com.example.mobile_embedded_system.domain.TacticalCommand;
import com.example.mobile_embedded_system.security.CryptoException;
import com.example.mobile_embedded_system.security.KeyStoreManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сетевой транспорт телеметрии на базе MQTT для приёма mesh-пакетов (ТЗ §4.1, §14, §19).
 */
public class MqttTransportManager {

    private static final String TAG = "MqttTransport";

    public static final byte FLAG_ENCRYPTED = 0x01;

    // Настройки сети (ТЗ §1.40) - временно хардкод для MVP P0
    private String networkRoot = "mesh-a";
    private String hierarchyPath = "7F10/21A0";

    private final TelemetryRepository repository;
    private final KeyStoreManager keyStoreManager;
    private final AtomicLong commandSequence = new AtomicLong(1L);
    private final PacketDiagnosticsModel diagnosticsModel =
            new PacketDiagnosticsModel();
    private MqttClient mqttClient;
    private boolean isConnecting = false;

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
    }

    public MqttTransportManager(TelemetryRepository repository) {
        this(repository, new KeyStoreManager());
    }

    public MqttTransportManager(TelemetryRepository repository, KeyStoreManager keyStoreManager) {
        this.repository = repository;
        this.keyStoreManager = (keyStoreManager != null) ? keyStoreManager : new KeyStoreManager();
    }

    public void setNetworkConfig(String root, String path) {
        this.networkRoot = root;
        this.hierarchyPath = path;
    }

    /**
     * Асинхронное подключение к брокеру телеметрии.
     */
    public synchronized void connect(String brokerUrl, String clientId, ConnectionCallback callback) {
        if (mqttClient != null && mqttClient.isConnected()) {
            if (callback != null) callback.onConnected();
            return;
        }

        if (isConnecting) {
            return;
        }

        isConnecting = true;

        new Thread(() -> {
            try {
                disconnectInternal();
                mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setConnectionTimeout(5);
                options.setKeepAliveInterval(10);

                mqttClient.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        Log.i(TAG, "MQTT подключен к: " + serverURI + " (reconnect=" + reconnect + ")");
                        diagnosticsModel.setConnectionState("ПОДКЛЮЧЕНО");
                        diagnosticsModel.setLastReconnectTimestampMs(System.currentTimeMillis());
                        diagnosticsModel.setBrokerUrl(serverURI);
                        diagnosticsModel.setActiveSubscriptionTopic(MqttTopicBuilder.buildSubtreeSubscriptionTopic(networkRoot, hierarchyPath));
                        subscribeToTelemetry();
                        if (callback != null) callback.onConnected();
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.w(TAG, "MQTT связь потеряна: " + (cause != null ? cause.getMessage() : "unknown"));
                        diagnosticsModel.setConnectionState("ОТКЛЮЧЕНО");
                        if (callback != null) callback.onDisconnected(cause != null ? cause.getMessage() : "Соединение разорвано");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        processIncomingMessage(topic, message.getPayload());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });

                mqttClient.connect(options);
                isConnecting = false;

            } catch (MqttException e) {
                isConnecting = false;
                Log.e(TAG, "Ошибка подключения к брокеру MQTT: " + e.getMessage(), e);
                if (callback != null) callback.onError("Сбой MQTT: " + e.getReasonCode());
            }
        }).start();
    }

    private void subscribeToTelemetry() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String topic = MqttTopicBuilder.buildSubtreeSubscriptionTopic(networkRoot, hierarchyPath);
                mqttClient.subscribe(topic, 1);
                Log.i(TAG, "Подписка оформлена на: " + topic);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Ошибка подписки на топик телеметрии", e);
        }
    }

    /**
     * Обработка входящего бинарного пакета с проверкой конверта шифрования (ТЗ §14, MVP §12.60–12.69).
     */
    public void processIncomingMessage(String topic, byte[] payloadBytes) {
        if (payloadBytes == null || payloadBytes.length < LMashPayload.PAYLOAD_SIZE) {
            diagnosticsModel.incrementMalformedPackets();
            Log.w(TAG, "Отброшен некорректный пакет. Длина: " + (payloadBytes != null ? payloadBytes.length : 0));
            return;
        }

        diagnosticsModel.incrementTotalPackets();

        byte[] unencryptedBytes = payloadBytes;

        // Проверка конверта шифрования: [1 байт флагов (0x01 = ENCRYPTED)][8 байт keyId][IV + CipherText]
        if (payloadBytes.length > LMashPayload.PAYLOAD_SIZE && (payloadBytes[0] & FLAG_ENCRYPTED) != 0) {
            try {
                ByteBuffer bb = ByteBuffer.wrap(payloadBytes, 1, 8);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                long keyId = bb.getLong();

                byte[] encryptedBody = new byte[payloadBytes.length - 9];
                System.arraycopy(payloadBytes, 9, encryptedBody, 0, encryptedBody.length);

                unencryptedBytes = keyStoreManager.decrypt(keyId, encryptedBody);
                Log.d(TAG, "Успешно расшифрован пакет по keyId=" + keyId);
            } catch (CryptoException ce) {
                diagnosticsModel.incrementDecryptionErrors();
                Log.e(TAG, "Ошибка расшифрования пакета (" + ce.getErrorCode().getDescription() + "): " + ce.getMessage());
                return;
            } catch (Exception e) {
                diagnosticsModel.incrementMalformedPackets();
                Log.e(TAG, "Ошибка разбора конверта шифрования пакета", e);
                return;
            }
        }

        try {
            LMashPayload payload = LMashPayload.fromBytes(unencryptedBytes);
            if (payload.isTelemetry()) {
                diagnosticsModel.incrementTelemetryPackets();
            } else if (payload.isCommand()) {
                diagnosticsModel.incrementCommandPackets();
                Log.d(TAG, "Пропущен пакет не-телеметрии (type=" + payload.getMessageType() + ")");
                return;
            }

            TelemetryEntity entity = convertToEntity(payload);
            repository.insert(entity, inserted -> {
                if (!inserted) {
                    diagnosticsModel.incrementDuplicatesDropped();
                    Log.d(TAG, "Отброшен дубликат пакета [" + entity.deviceSerial + "], seq=" + entity.sequence);
                } else {
                    Log.d(TAG, "Телеметрия сохранена от бойца [" + entity.userId + "], seq=" + entity.sequence);
                }
            });
        } catch (IllegalArgumentException e) {
            diagnosticsModel.incrementMalformedPackets();
            Log.e(TAG, "Ошибка декодирования бинарного пакета телеметрии", e);
        }
    }

    public PacketDiagnosticsModel getDiagnosticsModel() {
        return diagnosticsModel;
    }

    private TelemetryEntity convertToEntity(LMashPayload payload) {
        TelemetryEntity entity = new TelemetryEntity();
        entity.deviceSerial = payload.getDeviceSerial();
        entity.sequence = payload.getSequence();
        entity.userId = payload.getUserId();
        entity.destinationId = payload.getDestinationId();
        entity.timestamp = payload.getTimestamp();
        entity.latitude = payload.getLatitude();
        entity.longitude = payload.getLongitude();
        entity.headingDegrees = payload.getHeadingDegrees();
        entity.pulseBpm = payload.getPulseBpm();
        entity.temperatureCelsius = payload.getTemperatureCelsius();
        entity.pressureSys = payload.getPressureSys();
        entity.pressureDia = payload.getPressureDia();
        entity.positionQuality = payload.getPositionQuality();
        entity.receivedAtMs = System.currentTimeMillis();
        return entity;
    }

    private synchronized void disconnectInternal() {
        if (mqttClient != null) {
            try {
                mqttClient.setCallback(null);
                if (mqttClient.isConnected()) {
                    mqttClient.disconnectForcibly(1000L);
                }
                mqttClient.close();
            } catch (Exception e) {
                Log.w(TAG, "Ошибка закрытия предыдущего MQTT клиента: " + e.getMessage());
            } finally {
                mqttClient = null;
            }
        }
    }

    public synchronized void disconnect() {
        disconnectInternal();
        isConnecting = false;
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * Публикация командного пакета в бинарном виде LMashPayload (ТЗ §4.1, §14).
     */
    public boolean publishCommand(TacticalCommand command, long sourceId) {
        return publishCommand(command, sourceId, 0L);
    }

    public boolean publishCommand(TacticalCommand command, long sourceId, long keyId) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return false;
        }

        String topic = MqttTopicBuilder.buildCommandPublishTopic(
                networkRoot, hierarchyPath, command.getTargetUserId(), sourceId
        );

        try {
            long nextSequence = commandSequence.getAndIncrement();
            LMashPayload payload = command.toLMashPayload(nextSequence, sourceId);
            byte[] payloadBytes = payload.toBytes();

            if (keyId != 0L && keyStoreManager.getProfile(keyId) != null) {
                byte[] encryptedBody = keyStoreManager.encrypt(keyId, payloadBytes);
                ByteBuffer envelope = ByteBuffer.allocate(1 + 8 + encryptedBody.length);
                envelope.order(ByteOrder.LITTLE_ENDIAN);
                envelope.put(FLAG_ENCRYPTED);
                envelope.putLong(keyId);
                envelope.put(encryptedBody);
                payloadBytes = envelope.array();
                Log.i(TAG, "Зашифрована команда по keyId=" + keyId);
            }

            MqttMessage message = new MqttMessage(payloadBytes);
            message.setQos(1); // Гарантированная доставка приказа
            message.setRetained(false);

            mqttClient.publish(topic, message);
            Log.i(TAG, "Бинарная команда отправлена в топик: " + topic + " (seq=" + nextSequence + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка отправки команды", e);
            return false;
        }
    }
}
