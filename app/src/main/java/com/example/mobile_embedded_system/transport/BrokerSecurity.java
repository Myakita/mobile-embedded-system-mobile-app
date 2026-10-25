package com.example.mobile_embedded_system.transport;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Учётные данные и доверенный сертификат для брокера, который не входит в системное хранилище
 * (тестовый стенд: самоподписанный CA и логин/пароль). Без экземпляра этого класса
 * {@link MqttTransportManager} подключается как раньше.
 */
public final class BrokerSecurity {

    private final String username;
    private final char[] password;
    private final SSLSocketFactory socketFactory;

    private BrokerSecurity(String username, char[] password, SSLSocketFactory socketFactory) {
        this.username = username;
        this.password = password;
        this.socketFactory = socketFactory;
    }

    /**
     * @param username    логин или null, если брокер без авторизации
     * @param password    пароль, используется вместе с логином
     * @param trustedCaPem PEM-сертификат, которому доверяем вместо системных CA, или null
     */
    public static BrokerSecurity of(String username, String password, byte[] trustedCaPem)
            throws GeneralSecurityException, IOException {
        SSLSocketFactory factory = null;
        if (trustedCaPem != null) {
            Certificate ca = CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(trustedCaPem));
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            store.setCertificateEntry("stand-ca", ca);
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(store);
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, trust.getTrustManagers(), null);
            factory = context.getSocketFactory();
        }
        return new BrokerSecurity(username, password != null ? password.toCharArray() : null, factory);
    }

    /** Накладывается после настроек по умолчанию: логин/пароль и, если задан CA, свой TLS-контекст. */
    void applyTo(MqttConnectOptions options) {
        if (username != null) {
            options.setUserName(username);
            if (password != null) {
                options.setPassword(password);
            }
        }
        if (socketFactory != null) {
            options.setSocketFactory(socketFactory);
        }
    }
}
