package com.example.mobile_embedded_system.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Модульные тесты кодов ошибок и исключения шифрования (AC-08.2).
 */
public class CryptoExceptionTest {

    @Test
    public void errorCodesCoverAcceptanceCriteriaSet() {
        Set<String> names = new HashSet<>();
        for (CryptoErrorCode c : CryptoErrorCode.values()) names.add(c.name());
        Set<String> expected = new HashSet<>(java.util.Arrays.asList(
                "UNKNOWN_KEY", "DECRYPTION_FAILED", "AUTHENTICATION_FAILED", "UNSUPPORTED_CRYPTO"));
        assertEquals(expected, names);
    }

    @Test
    public void errorCodeDescriptionsAreNonEmptyAndDistinct() {
        Set<String> seen = new HashSet<>();
        for (CryptoErrorCode c : CryptoErrorCode.values()) {
            assertFalse(c.getDescription().isEmpty());
            assertTrue(seen.add(c.getDescription()));
        }
    }

    @Test
    public void messageIsDescriptionPlusDetail() {
        CryptoException e = new CryptoException(CryptoErrorCode.AUTHENTICATION_FAILED, "tag mismatch");
        assertEquals(CryptoErrorCode.AUTHENTICATION_FAILED.getDescription() + ": tag mismatch", e.getMessage());
        assertEquals(CryptoErrorCode.AUTHENTICATION_FAILED, e.getErrorCode());
        assertNull(e.getCause());
    }

    @Test
    public void causeIsPreserved() {
        Throwable cause = new IllegalStateException("boom");
        CryptoException e = new CryptoException(CryptoErrorCode.DECRYPTION_FAILED, "bad", cause);
        assertSame(cause, e.getCause());
        assertEquals(CryptoErrorCode.DECRYPTION_FAILED, e.getErrorCode());
    }

    @Test
    public void exceptionIsChecked() {
        assertFalse(RuntimeException.class.isAssignableFrom(CryptoException.class));
        assertTrue(Exception.class.isAssignableFrom(CryptoException.class));
    }

    @Test
    public void everyCodeCanBeCarried() {
        for (CryptoErrorCode c : CryptoErrorCode.values()) {
            assertEquals(c, new CryptoException(c, "x").getErrorCode());
        }
    }
}
