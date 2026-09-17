package com.example.mobile_embedded_system.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Модульное тестирование менеджера ключей и криптографического модуля (ТЗ §14, MVP §12.60–12.70).
 */
public class KeyStoreManagerTest {

    private KeyStoreManager keyStoreManager;

    @Before
    public void setUp() {
        keyStoreManager = new KeyStoreManager();
    }

    @Test
    public void testAes128GcmEncryptionAndDecryption() throws Exception {
        long keyId = 0xA101L;
        byte[] rawKey = new byte[16]; // 128 бит
        for (int i = 0; i < rawKey.length; i++) rawKey[i] = (byte) (i + 1);

        keyStoreManager.importKeyToKeyStore("alias_aes_128", CryptoAlgorithm.AES_128_GCM, rawKey);
        CryptoProfile profile = new CryptoProfile("PROF_AES_128", keyId, CryptoAlgorithm.AES_128_GCM, "alias_aes_128", 1001L);
        keyStoreManager.registerProfile(profile);

        byte[] plaintext = "TELEMETRY_PAYLOAD_TEST_DATA_128".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = keyStoreManager.encrypt(keyId, plaintext);
        assertNotNull(encrypted);

        byte[] decrypted = keyStoreManager.decrypt(keyId, encrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void testAes256GcmEncryptionAndDecryption() throws Exception {
        long keyId = 0xA256L;
        byte[] rawKey = new byte[32]; // 256 бит
        for (int i = 0; i < rawKey.length; i++) rawKey[i] = (byte) (i + 10);

        keyStoreManager.importKeyToKeyStore("alias_aes_256", CryptoAlgorithm.AES_256_GCM, rawKey);
        CryptoProfile profile = new CryptoProfile("PROF_AES_256", keyId, CryptoAlgorithm.AES_256_GCM, "alias_aes_256", 1002L);
        keyStoreManager.registerProfile(profile);

        byte[] plaintext = "TACTICAL_COMMAND_PAYLOAD_256".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = keyStoreManager.encrypt(keyId, plaintext);
        assertNotNull(encrypted);

        byte[] decrypted = keyStoreManager.decrypt(keyId, encrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void testChaCha20Poly1305EncryptionAndDecryption() throws Exception {
        long keyId = 0xCC20L;
        byte[] rawKey = new byte[32]; // 256 бит
        for (int i = 0; i < rawKey.length; i++) rawKey[i] = (byte) (i + 5);

        keyStoreManager.importKeyToKeyStore("alias_chacha20", CryptoAlgorithm.CHACHA20_POLY1305, rawKey);
        CryptoProfile profile = new CryptoProfile("PROF_CHACHA", keyId, CryptoAlgorithm.CHACHA20_POLY1305, "alias_chacha20", 0L);
        keyStoreManager.registerProfile(profile);

        byte[] plaintext = "CHACHA20_POLY1305_WIRE_FORMAT".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = keyStoreManager.encrypt(keyId, plaintext);
        assertNotNull(encrypted);

        byte[] decrypted = keyStoreManager.decrypt(keyId, encrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void testUnknownKeyIdThrowsCryptoException() {
        try {
            keyStoreManager.decrypt(0x9999L, new byte[]{1, 2, 3, 4, 5});
            fail("Ожидалось исключение CryptoException при неверном keyId");
        } catch (CryptoException e) {
            assertEquals(CryptoErrorCode.UNKNOWN_KEY, e.getErrorCode());
        }
    }

    @Test
    public void testCorruptedCiphertextThrowsAuthenticationOrDecryptionError() throws Exception {
        long keyId = 0xAEADL;
        byte[] rawKey = new byte[16];
        keyStoreManager.importKeyToKeyStore("alias_aead_corrupt", CryptoAlgorithm.AES_128_GCM, rawKey);
        CryptoProfile profile = new CryptoProfile("PROF_AEAD", keyId, CryptoAlgorithm.AES_128_GCM, "alias_aead_corrupt", 0L);
        keyStoreManager.registerProfile(profile);

        byte[] plaintext = "AEAD_INTEGRITY_CHECK".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = keyStoreManager.encrypt(keyId, plaintext);

        // Повреждаем шифротекст (последний байт тега)
        encrypted[encrypted.length - 1] ^= 0xFF;

        try {
            keyStoreManager.decrypt(keyId, encrypted);
            fail("Ожидалось исключение проверки целостности");
        } catch (CryptoException e) {
            assertTrue(e.getErrorCode() == CryptoErrorCode.AUTHENTICATION_FAILED || e.getErrorCode() == CryptoErrorCode.DECRYPTION_FAILED);
        }
    }
}
