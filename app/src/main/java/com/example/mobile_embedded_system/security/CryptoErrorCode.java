package com.example.mobile_embedded_system.security;

/**
 * Коды ошибок шифрования и аутентификации сообщений по ТЗ (§14, MVP §12.66).
 */
public enum CryptoErrorCode {
    UNKNOWN_KEY("Нет подходящего ключа"),
    DECRYPTION_FAILED("Ошибка расшифрования"),
    AUTHENTICATION_FAILED("Ошибка проверки целостности (AEAD tag mismatch)"),
    UNSUPPORTED_CRYPTO("Неподдерживаемый алгоритм шифрования");

    private final String description;

    CryptoErrorCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
