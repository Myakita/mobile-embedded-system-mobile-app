package com.example.mobile_embedded_system.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Модульные тесты профиля шифрования (ТЗ §14, MVP §12.60–12.65).
 */
public class CryptoProfileTest {

    private static CryptoProfile profile() {
        return new CryptoProfile("p1", 7L, CryptoAlgorithm.AES_256_GCM, "alias-1", 430572065L);
    }

    @Test
    public void gettersReturnConstructorValues() {
        long before = System.currentTimeMillis();
        CryptoProfile p = profile();
        assertEquals("p1", p.getProfileId());
        assertEquals(7L, p.getKeyId());
        assertEquals(CryptoAlgorithm.AES_256_GCM, p.getAlgorithm());
        assertEquals("alias-1", p.getKeyAlias());
        assertEquals(430572065L, p.getDeviceSerial());
        assertTrue(p.getCreatedAtMs() >= before);
    }

    @Test
    public void equalProfilesIgnoreCreationTime() throws Exception {
        CryptoProfile a = profile();
        Thread.sleep(3);
        CryptoProfile b = profile();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentKeyIdMakesProfilesDifferent() {
        assertNotEquals(profile(), new CryptoProfile("p1", 8L, CryptoAlgorithm.AES_256_GCM, "alias-1", 430572065L));
    }

    @Test
    public void differentAlgorithmMakesProfilesDifferent() {
        assertNotEquals(profile(), new CryptoProfile("p1", 7L, CryptoAlgorithm.AES_128_GCM, "alias-1", 430572065L));
    }

    @Test
    public void differentDeviceMakesProfilesDifferent() {
        assertNotEquals(profile(), new CryptoProfile("p1", 7L, CryptoAlgorithm.AES_256_GCM, "alias-1", 1L));
    }

    @Test
    public void notEqualToNullOrOtherType() {
        assertNotEquals(profile(), null);
        assertNotEquals(profile(), "p1");
    }

    @Test
    public void nullFieldsAreAllowedInEquality() {
        CryptoProfile a = new CryptoProfile(null, 1L, null, null, 1L);
        CryptoProfile b = new CryptoProfile(null, 1L, null, null, 1L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
