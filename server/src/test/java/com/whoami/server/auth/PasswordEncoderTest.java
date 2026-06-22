package com.whoami.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordEncoderTest {

    @Test
    public void testEncodeAndMatch() {
        String rawPassword = "mySecretPassword123";
        
        String encoded = PasswordEncoder.encode(rawPassword);
        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        
        // Correct password
        assertTrue(PasswordEncoder.matches(rawPassword, encoded), "Password should match its encoded hash");
        
        // Incorrect password
        assertFalse(PasswordEncoder.matches("wrongPassword", encoded), "Wrong password should not match the hash");
    }

    @Test
    public void testSaltUniqueness() {
        String rawPassword = "password";
        
        String hash1 = PasswordEncoder.encode(rawPassword);
        String hash2 = PasswordEncoder.encode(rawPassword);
        
        // Bcrypt generates a new salt every time, so hashes of the same string must differ
        assertNotEquals(hash1, hash2, "Bcrypt hashes should differ due to unique salts");
        
        // But both should be valid
        assertTrue(PasswordEncoder.matches(rawPassword, hash1));
        assertTrue(PasswordEncoder.matches(rawPassword, hash2));
    }

    @Test
    public void testNullPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.encode(null), "Should throw exception if password is null");
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.matches(null, "somehash"), "Should throw exception if password is null");
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.matches("password", null), "Should throw exception if hash is null");
    }
}
