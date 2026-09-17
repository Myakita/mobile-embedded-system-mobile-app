package com.example.mobile_embedded_system.security;

/**
 * Исключение криптографической обработки по ТЗ (§14, MVP §12.66).
 */
public class CryptoException extends Exception {

    private final CryptoErrorCode errorCode;

    public CryptoException(CryptoErrorCode errorCode, String message) {
        super(errorCode.getDescription() + ": " + message);
        this.errorCode = errorCode;
    }

    public CryptoException(CryptoErrorCode errorCode, String message, Throwable cause) {
        super(errorCode.getDescription() + ": " + message, cause);
        this.errorCode = errorCode;
    }

    public CryptoErrorCode getErrorCode() {
        return errorCode;
    }
}
