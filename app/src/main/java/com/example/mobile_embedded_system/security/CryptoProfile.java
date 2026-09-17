package com.example.mobile_embedded_system.security;

import java.util.Objects;

/**
 * Профиль шифрования приложения-получателя (CryptoProfile) по ТЗ (§14, MVP §12.60–12.65).
 */
public class CryptoProfile {

    private final String profileId;
    private final long keyId;
    private final CryptoAlgorithm algorithm;
    private final String keyAlias;
    private final long deviceSerial;
    private final long createdAtMs;

    public CryptoProfile(String profileId, long keyId, CryptoAlgorithm algorithm, String keyAlias, long deviceSerial) {
        this.profileId = profileId;
        this.keyId = keyId;
        this.algorithm = algorithm;
        this.keyAlias = keyAlias;
        this.deviceSerial = deviceSerial;
        this.createdAtMs = System.currentTimeMillis();
    }

    public String getProfileId() {
        return profileId;
    }

    public long getKeyId() {
        return keyId;
    }

    public CryptoAlgorithm getAlgorithm() {
        return algorithm;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public long getDeviceSerial() {
        return deviceSerial;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CryptoProfile profile = (CryptoProfile) o;
        return keyId == profile.keyId &&
                deviceSerial == profile.deviceSerial &&
                Objects.equals(profileId, profile.profileId) &&
                algorithm == profile.algorithm &&
                Objects.equals(keyAlias, profile.keyAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId, keyId, algorithm, keyAlias, deviceSerial);
    }
}
