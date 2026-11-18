package com.example.mobile_embedded_system.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Модульные тесты перечня алгоритмов шифрования (ТЗ §14, MVP §12.62, AC-08).
 */
public class CryptoAlgorithmTest {

    @Test
    public void fromNameFindsByDisplayName() {
        assertSame(CryptoAlgorithm.AES_128_GCM, CryptoAlgorithm.fromName("AES-128-GCM"));
        assertSame(CryptoAlgorithm.AES_256_GCM, CryptoAlgorithm.fromName("AES-256-GCM"));
        assertSame(CryptoAlgorithm.CHACHA20_POLY1305, CryptoAlgorithm.fromName("ChaCha20-Poly1305"));
    }

    @Test
    public void fromNameIsCaseInsensitive() {
        assertSame(CryptoAlgorithm.AES_256_GCM, CryptoAlgorithm.fromName("aes-256-gcm"));
        assertSame(CryptoAlgorithm.CHACHA20_POLY1305, CryptoAlgorithm.fromName("CHACHA20-POLY1305"));
    }

    @Test
    public void fromNameAcceptsEnumConstantName() {
        assertSame(CryptoAlgorithm.AES_128_GCM, CryptoAlgorithm.fromName("AES_128_GCM"));
        assertSame(CryptoAlgorithm.CHACHA20_POLY1305, CryptoAlgorithm.fromName("chacha20_poly1305"));
    }

    @Test
    public void unknownNameGivesNull() {
        assertNull(CryptoAlgorithm.fromName("DES"));
        assertNull(CryptoAlgorithm.fromName(""));
        assertNull(CryptoAlgorithm.fromName(" AES-128-GCM"));
    }

    @Test
    public void nullNameGivesNull() {
        assertNull(CryptoAlgorithm.fromName(null));
    }

    @Test
    public void keySizesAndNonceLengths() {
        assertEquals(128, CryptoAlgorithm.AES_128_GCM.getKeySizeBits());
        assertEquals(256, CryptoAlgorithm.AES_256_GCM.getKeySizeBits());
        assertEquals(256, CryptoAlgorithm.CHACHA20_POLY1305.getKeySizeBits());
        for (CryptoAlgorithm a : CryptoAlgorithm.values()) {
            assertEquals(12, a.getIvSizeBytes());
        }
    }

    @Test
    public void transformationStringsMatchJcaNames() {
        assertEquals("AES/GCM/NoPadding", CryptoAlgorithm.AES_128_GCM.getTransformation());
        assertEquals("AES/GCM/NoPadding", CryptoAlgorithm.AES_256_GCM.getTransformation());
        assertEquals("ChaCha20-Poly1305", CryptoAlgorithm.CHACHA20_POLY1305.getTransformation());
    }

    @Test
    public void displayNames() {
        assertEquals("AES-128-GCM", CryptoAlgorithm.AES_128_GCM.getName());
        assertEquals("ChaCha20-Poly1305", CryptoAlgorithm.CHACHA20_POLY1305.getName());
        assertEquals(3, CryptoAlgorithm.values().length);
    }
}
