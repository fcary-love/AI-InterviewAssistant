package com.faceai.pdfreader.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hash_returnsDifferentHashes_forSameInput() {
        String hash1 = passwordService.hash("password123");
        String hash2 = passwordService.hash("password123");
        assertNotEquals(hash1, hash2, "salt should make hashes different");
    }

    @Test
    void matches_returnsTrue_forCorrectPassword() {
        String hash = passwordService.hash("mySecret");
        assertTrue(passwordService.matches("mySecret", hash));
    }

    @Test
    void matches_returnsFalse_forWrongPassword() {
        String hash = passwordService.hash("mySecret");
        assertFalse(passwordService.matches("wrongPassword", hash));
    }

    @Test
    void matches_returnsFalse_forNullRawPassword() {
        String hash = passwordService.hash("mySecret");
        assertFalse(passwordService.matches(null, hash));
    }

    @Test
    void matches_returnsFalse_forNullStoredHash() {
        assertFalse(passwordService.matches("password", null));
    }

    @Test
    void matches_returnsFalse_forMalformedHash() {
        assertFalse(passwordService.matches("password", "not-a-valid-hash"));
    }

    @Test
    void matches_returnsFalse_forEmptyHash() {
        assertFalse(passwordService.matches("password", ""));
    }

    @Test
    void hash_outputContainsThreeParts() {
        String hash = passwordService.hash("test");
        String[] parts = hash.split(":");
        assertEquals(3, parts.length, "hash should have iterations:salt:key format");
    }
}
