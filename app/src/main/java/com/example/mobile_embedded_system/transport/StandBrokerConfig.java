package com.example.mobile_embedded_system.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * Подключение к брокеру тестового стенда (репозиторий эмулятора, {@code emulator/stand}).
 * Стенд кладёт в каталог приложения два файла: {@code broker.properties} (host, port, user,
 * password) и {@code ca.crt}. Если {@code broker.properties} нет, как на обычном устройстве,
 * класс ничего не меняет в работе приложения.
 */
public final class StandBrokerConfig {

    public static final String DIR_NAME = "stand";
    public static final String PROPERTIES_FILE = "broker.properties";
    public static final String CA_FILE = "ca.crt";

    private final String brokerUrl;
    private final BrokerSecurity security;

    private StandBrokerConfig(String brokerUrl, BrokerSecurity security) {
        this.brokerUrl = brokerUrl;
        this.security = security;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public BrokerSecurity getSecurity() {
        return security;
    }

    /**
     * @return конфигурация стенда или null, если в {@code dir} нет {@code broker.properties}
     * @throws IllegalArgumentException файл есть, но в нём ошибка (нет host, плохой порт, нет или
     *                                  не читается ca.crt): стенд не должен молча уйти на другой брокер
     */
    public static StandBrokerConfig load(File dir) throws IOException {
        File propertiesFile = new File(dir, PROPERTIES_FILE);
        if (!propertiesFile.isFile()) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(propertiesFile)) {
            properties.load(in);
        }
        File caFile = new File(dir, CA_FILE);
        if (!caFile.isFile()) {
            throw new IllegalArgumentException(propertiesFile + " есть, а " + CA_FILE + " рядом нет");
        }
        return parse(properties, Files.readAllBytes(caFile.toPath()));
    }

    static StandBrokerConfig parse(Properties properties, byte[] caPem) {
        String host = trimmed(properties, "host");
        if (host == null) {
            throw new IllegalArgumentException("broker.properties: не задан host");
        }
        int port = parsePort(trimmed(properties, "port"));
        try {
            BrokerSecurity security = BrokerSecurity.of(
                    trimmed(properties, "user"), properties.getProperty("password"), caPem);
            return new StandBrokerConfig("ssl://" + host + ":" + port, security);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(CA_FILE + ": не удалось прочитать сертификат: " + e.getMessage(), e);
        }
    }

    private static int parsePort(String value) {
        if (value == null) {
            return 8883;
        }
        try {
            int port = Integer.parseInt(value);
            if (port > 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // сообщение ниже
        }
        throw new IllegalArgumentException("broker.properties: port должен быть 1-65535, а не '" + value + "'");
    }

    private static String trimmed(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
