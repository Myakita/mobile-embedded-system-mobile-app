package com.example.mobile_embedded_system.transport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Конфигурация брокера тестового стенда: разбор файлов, доверенный CA и логин/пароль.
 */
public class StandBrokerConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static byte[] caPem() throws IOException {
        try (InputStream in = StandBrokerConfigTest.class.getResourceAsStream("/stand-ca.crt")) {
            assertNotNull("stand-ca.crt must be a test resource", in);
            return in.readAllBytes();
        }
    }

    private static Properties properties(String... keysAndValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }

    @Test
    public void parseBuildsSslUrlAndAppliesCredentialsAndTrust() throws Exception {
        StandBrokerConfig config = StandBrokerConfig.parse(
                properties("host", "10.0.2.2", "port", "8883", "user", "app", "password", "app-test-pw"), caPem());

        assertEquals("ssl://10.0.2.2:8883", config.getBrokerUrl());
        MqttConnectOptions options = new MqttConnectOptions();
        config.getSecurity().applyTo(options);
        assertEquals("app", options.getUserName());
        assertArrayEquals("app-test-pw".toCharArray(), options.getPassword());
        assertNotNull("the stand CA must replace the system trust store", options.getSocketFactory());
    }

    @Test
    public void parseDefaultsPortAndTrimsValues() throws Exception {
        StandBrokerConfig config = StandBrokerConfig.parse(properties("host", " 10.0.2.2 "), caPem());

        assertEquals("ssl://10.0.2.2:8883", config.getBrokerUrl());
    }

    @Test
    public void parseRejectsMissingHostBadPortAndBadCertificate() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> StandBrokerConfig.parse(properties("port", "8883"), caPem()));
        assertThrows(IllegalArgumentException.class,
                () -> StandBrokerConfig.parse(properties("host", "h", "port", "70000"), caPem()));
        assertThrows(IllegalArgumentException.class,
                () -> StandBrokerConfig.parse(properties("host", "h", "port", "abc"), caPem()));
        assertThrows(IllegalArgumentException.class,
                () -> StandBrokerConfig.parse(properties("host", "h"), "not a certificate".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void securityWithoutCredentialsAndCaLeavesOptionsUntouched() throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        BrokerSecurity.of(null, null, null).applyTo(options);

        assertNull(options.getUserName());
        assertNull(options.getPassword());
        assertNull(options.getSocketFactory());
    }

    @Test
    public void loadReturnsNullWhenThereIsNoStandConfiguration() throws Exception {
        assertNull(StandBrokerConfig.load(folder.newFolder("empty")));
    }

    @Test
    public void loadReadsBothFiles() throws Exception {
        File dir = folder.newFolder("stand");
        Files.write(new File(dir, StandBrokerConfig.PROPERTIES_FILE).toPath(),
                "# written by emulator/stand/lib.sh\nhost=10.0.2.2\nport=8883\nuser=app\npassword=app-test-pw\n"
                        .getBytes(StandardCharsets.UTF_8));
        Files.write(new File(dir, StandBrokerConfig.CA_FILE).toPath(), caPem());

        StandBrokerConfig config = StandBrokerConfig.load(dir);

        assertNotNull(config);
        assertEquals("ssl://10.0.2.2:8883", config.getBrokerUrl());
    }

    @Test
    public void loadFailsLoudlyWhenTheCertificateIsMissing() throws Exception {
        File dir = folder.newFolder("stand");
        Files.write(new File(dir, StandBrokerConfig.PROPERTIES_FILE).toPath(),
                "host=10.0.2.2\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> StandBrokerConfig.load(dir));
    }
}
