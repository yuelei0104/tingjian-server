package com.tingjian.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTests {
    @Test
    void hashUsesRandomSaltAndMatchesOriginalPassword() {
        String first = PasswordHasher.hash("correct-password");
        String second = PasswordHasher.hash("correct-password");

        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("correct-password", first));
    }

    @Test
    void rejectsWrongPasswordAndMalformedHash() {
        String hash = PasswordHasher.hash("correct-password");

        assertFalse(PasswordHasher.matches("wrong-password", hash));
        assertFalse(PasswordHasher.matches("correct-password", "invalid"));
    }
}
