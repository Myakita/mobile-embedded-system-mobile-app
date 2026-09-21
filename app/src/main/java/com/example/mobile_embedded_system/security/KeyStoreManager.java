package com.example.mobile_embedded_system.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Менеджер ключей и криптографических операций на базе AndroidKeyStore (ТЗ §14, MVP §12.60–12.70).
 */
public class KeyStoreManager {

    private static final String TAG = "KeyStoreManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private final KeyStore keyStore;
    private final Map<Long, CryptoProfile> profilesByKeyId = new HashMap<>();
    private final Map<String, SecretKey> memoryKeyStore = new HashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyStoreManager() {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(KEYSTORE_PROVIDER);
            ks.load(null);
        } catch (Exception e) {
            Log.w(TAG, "AndroidKeyStore не доступен в текущем окружении: " + e.getMessage());
            ks = null;
        }
        this.keyStore = ks;
        initDefaultDemoProfiles();
    }

    public synchronized void initDefaultDemoProfiles() {
        long[] demoKeyIds = {1L, 1001L, 1002L, 1003L};
        for (long keyId : demoKeyIds) {
            String alias = "demo_key_" + keyId;
            try {
                generateKeyInKeyStore(alias, CryptoAlgorithm.AES_128_GCM);
                CryptoProfile profile = new CryptoProfile("profile_" + keyId, keyId, CryptoAlgorithm.AES_128_GCM, alias, 99881100L + keyId);
                registerProfile(profile);
            } catch (CryptoException e) {
                Log.w(TAG, "Ошибка регистрации демо-ключа keyId=" + keyId + ": " + e.getMessage());
            }
        }
    }

    public synchronized void registerProfile(CryptoProfile profile) {
        if (profile != null) {
            profilesByKeyId.put(profile.getKeyId(), profile);
            Log.i(TAG, "Зарегистрирован CryptoProfile: id=" + profile.getProfileId() + ", keyId=" + profile.getKeyId() + ", algo=" + profile.getAlgorithm().getName());
        }
    }

    public synchronized void unregisterProfile(long keyId) {
        CryptoProfile profile = profilesByKeyId.remove(keyId);
        if (profile != null) {
            memoryKeyStore.remove(profile.getKeyAlias());
            if (keyStore != null) {
                try {
                    if (keyStore.containsAlias(profile.getKeyAlias())) {
                        keyStore.deleteEntry(profile.getKeyAlias());
                        Log.i(TAG, "Удален ключ из AndroidKeyStore: " + profile.getKeyAlias());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Ошибка удаления ключа из AndroidKeyStore: " + e.getMessage());
                }
            }
        }
    }

    public synchronized CryptoProfile getProfile(long keyId) {
        return profilesByKeyId.get(keyId);
    }

    public synchronized SecretKey generateKeyInKeyStore(String alias, CryptoAlgorithm algorithm) throws CryptoException {
        if (algorithm == null) {
            throw new CryptoException(CryptoErrorCode.UNSUPPORTED_CRYPTO, "Алгоритм не указан");
        }

        try {
            if (algorithm == CryptoAlgorithm.AES_128_GCM || algorithm == CryptoAlgorithm.AES_256_GCM) {
                if (keyStore != null) {
                    try {
                        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_AES,
                                KEYSTORE_PROVIDER
                        );

                        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                                alias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                        )
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setKeySize(algorithm.getKeySizeBits())
                                .setRandomizedEncryptionRequired(true)
                                .build();

                        keyGenerator.init(spec);
                        SecretKey key = keyGenerator.generateKey();
                        memoryKeyStore.put(alias, key);
                        return key;
                    } catch (Exception e) {
                        Log.w(TAG, "Ошибка создания ключа в AndroidKeyStore, fallback к памяти: " + e.getMessage());
                    }
                }

                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(algorithm.getKeySizeBits());
                SecretKey key = keyGen.generateKey();
                memoryKeyStore.put(alias, key);
                return key;
            } else if (algorithm == CryptoAlgorithm.CHACHA20_POLY1305) {
                byte[] rawKey = new byte[32];
                secureRandom.nextBytes(rawKey);
                return importKeyToKeyStore(alias, algorithm, rawKey);
            } else {
                throw new CryptoException(CryptoErrorCode.UNSUPPORTED_CRYPTO, "Неподдерживаемый алгоритм: " + algorithm.getName());
            }
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.UNSUPPORTED_CRYPTO, "Ошибка генерации ключа " + alias + ": " + e.getMessage(), e);
        }
    }

    public synchronized SecretKey importKeyToKeyStore(String alias, CryptoAlgorithm algorithm, byte[] rawKeyBytes) throws CryptoException {
        if (rawKeyBytes == null || rawKeyBytes.length == 0) {
            throw new CryptoException(CryptoErrorCode.UNSUPPORTED_CRYPTO, "Ключ пуст");
        }

        try {
            SecretKey secretKey;
            if (algorithm == CryptoAlgorithm.CHACHA20_POLY1305) {
                secretKey = new SecretKeySpec(rawKeyBytes, "ChaCha20");
            } else {
                secretKey = new SecretKeySpec(rawKeyBytes, "AES");
            }

            memoryKeyStore.put(alias, secretKey);

            if (keyStore != null) {
                try {
                    KeyProtection.Builder builder = new KeyProtection.Builder(
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                    );
                    if (algorithm == CryptoAlgorithm.AES_128_GCM || algorithm == CryptoAlgorithm.AES_256_GCM) {
                        builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                               .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
                    }
                    keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), builder.build());
                } catch (NoClassDefFoundError | Exception e) {
                    Log.w(TAG, "Ошибка записи в AndroidKeyStore: " + e.getMessage());
                }
            }
            return secretKey;
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.UNSUPPORTED_CRYPTO, "Ошибка импорта ключа: " + e.getMessage(), e);
        }
    }

    public synchronized SecretKey getKeyFromKeyStore(String alias, CryptoAlgorithm algorithm) throws CryptoException {
        if (keyStore != null) {
            try {
                if (keyStore.containsAlias(alias)) {
                    KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
                    if (entry != null) {
                        return entry.getSecretKey();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Ошибка чтения ключа из AndroidKeyStore: " + e.getMessage());
            }
        }

        SecretKey inMemoryKey = memoryKeyStore.get(alias);
        if (inMemoryKey != null) {
            return inMemoryKey;
        }

        throw new CryptoException(CryptoErrorCode.UNKNOWN_KEY, "Ключ с псевдонимом '" + alias + "' не найден");
    }

    public byte[] encrypt(long keyId, byte[] plaintext) throws CryptoException {
        CryptoProfile profile = getProfile(keyId);
        if (profile == null) {
            throw new CryptoException(CryptoErrorCode.UNKNOWN_KEY, "Профиль с keyId=" + keyId + " не зарегистрирован");
        }

        if (plaintext == null || plaintext.length == 0) {
            return new byte[0];
        }

        SecretKey secretKey = getKeyFromKeyStore(profile.getKeyAlias(), profile.getAlgorithm());
        CryptoAlgorithm algo = profile.getAlgorithm();

        try {
            Cipher cipher = Cipher.getInstance(algo.getTransformation());
            byte[] iv = new byte[algo.getIvSizeBytes()];
            secureRandom.nextBytes(iv);

            if (algo == CryptoAlgorithm.AES_128_GCM || algo == CryptoAlgorithm.AES_256_GCM) {
                GCMParameterSpec spec = new GCMParameterSpec(algo.getTagSizeBytes(), iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            } else if (algo == CryptoAlgorithm.CHACHA20_POLY1305) {
                IvParameterSpec spec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            }

            byte[] cipherText = cipher.doFinal(plaintext);

            byte[] result = new byte[1 + iv.length + cipherText.length];
            result[0] = (byte) (iv.length & 0xFF);
            System.arraycopy(iv, 0, result, 1, iv.length);
            System.arraycopy(cipherText, 0, result, 1 + iv.length, cipherText.length);

            return result;
        } catch (Exception e) {
            throw new CryptoException(CryptoErrorCode.DECRYPTION_FAILED, "Ошибка зашифрования: " + e.getMessage(), e);
        }
    }

    public byte[] decrypt(long keyId, byte[] encryptedPayload) throws CryptoException {
        CryptoProfile profile = getProfile(keyId);
        if (profile == null) {
            throw new CryptoException(CryptoErrorCode.UNKNOWN_KEY, "Ключ keyId=" + keyId + " не зарегистрирован в CryptoProfile");
        }

        if (encryptedPayload == null || encryptedPayload.length <= 1) {
            throw new CryptoException(CryptoErrorCode.DECRYPTION_FAILED, "Короткий или пустой шифротекст");
        }

        SecretKey secretKey = getKeyFromKeyStore(profile.getKeyAlias(), profile.getAlgorithm());
        CryptoAlgorithm algo = profile.getAlgorithm();

        try {
            int ivLength = encryptedPayload[0] & 0xFF;
            if (ivLength <= 0 || ivLength > 32 || (1 + ivLength) >= encryptedPayload.length) {
                throw new CryptoException(CryptoErrorCode.DECRYPTION_FAILED, "Некорректная длина IV в пакете");
            }

            byte[] iv = new byte[ivLength];
            System.arraycopy(encryptedPayload, 1, iv, 0, ivLength);

            int cipherTextLen = encryptedPayload.length - 1 - ivLength;
            byte[] cipherText = new byte[cipherTextLen];
            System.arraycopy(encryptedPayload, 1 + ivLength, cipherText, 0, cipherTextLen);

            Cipher cipher = Cipher.getInstance(algo.getTransformation());

            if (algo == CryptoAlgorithm.AES_128_GCM || algo == CryptoAlgorithm.AES_256_GCM) {
                GCMParameterSpec spec = new GCMParameterSpec(algo.getTagSizeBytes(), iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            } else if (algo == CryptoAlgorithm.CHACHA20_POLY1305) {
                IvParameterSpec spec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            }

            return cipher.doFinal(cipherText);
        } catch (AEADBadTagException ae) {
            throw new CryptoException(CryptoErrorCode.AUTHENTICATION_FAILED, "Ошибка проверки целостности AEAD (mismatch tag)", ae);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("tag")) {
                throw new CryptoException(CryptoErrorCode.AUTHENTICATION_FAILED, "Ошибка проверки целостности (AEAD tag mismatch)", e);
            }
            throw new CryptoException(CryptoErrorCode.DECRYPTION_FAILED, "Ошибка расшифрования пакета: " + e.getMessage(), e);
        }
    }
}
