package com.example.mobile_embedded_system.security;

/**
 * Перечень стандартных алгоритмов шифрования по ТЗ (§14, MVP §12.62).
 */
public enum CryptoAlgorithm {
    AES_128_GCM("AES-128-GCM", "AES/GCM/NoPadding", 128, 12, 128),
    AES_256_GCM("AES-256-GCM", "AES/GCM/NoPadding", 256, 12, 128),
    CHACHA20_POLY1305("ChaCha20-Poly1305", "ChaCha20-Poly1305", 256, 12, 128);

    private final String name;
    private final String transformation;
    private final int keySizeBits;
    private final int ivSizeBytes;
    private final int tagSizeBytes;

    CryptoAlgorithm(String name, String transformation, int keySizeBits, int ivSizeBytes, int tagSizeBytes) {
        this.name = name;
        this.transformation = transformation;
        this.keySizeBits = keySizeBits;
        this.ivSizeBytes = ivSizeBytes;
        this.tagSizeBytes = tagSizeBytes;
    }

    public String getName() {
        return name;
    }

    public String getTransformation() {
        return transformation;
    }

    public int getKeySizeBits() {
        return keySizeBits;
    }

    public int getIvSizeBytes() {
        return ivSizeBytes;
    }

    public int getTagSizeBytes() {
        return tagSizeBytes;
    }

    public static CryptoAlgorithm fromName(String name) {
        if (name == null) return null;
        for (CryptoAlgorithm algo : values()) {
            if (algo.name.equalsIgnoreCase(name) || algo.name().equalsIgnoreCase(name)) {
                return algo;
            }
        }
        return null;
    }
}
