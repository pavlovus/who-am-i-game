package com.whoami.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtProviderTest {

    @Test
    public void testGenerateAndValidateToken() {
        int userId = 100;
        String username = "testuser";

        String token = JwtProvider.generateAccessToken(userId, username);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // Token should be valid
        assertTrue(JwtProvider.validateToken(token), "Generated token must be valid");
    }

    @Test
    public void testExtractUserId() {
        int userId = 250;
        String username = "gamer99";

        String token = JwtProvider.generateAccessToken(userId, username);
        
        int extractedId = JwtProvider.getUserIdFromToken(token);
        assertEquals(userId, extractedId, "Extracted User ID must match the original ID");
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.payload";
        
        assertFalse(JwtProvider.validateToken(invalidToken), "Invalid or tampered token should return false");
        assertThrows(Exception.class, () -> JwtProvider.getUserIdFromToken(invalidToken), "Should throw exception when trying to parse invalid token");
    }

    @Test
    public void testNullOrEmptyToken() {
        assertFalse(JwtProvider.validateToken(null), "Null token should be invalid");
        assertFalse(JwtProvider.validateToken(""), "Empty token should be invalid");
        assertFalse(JwtProvider.validateToken("   "), "Blank token should be invalid");
    }
}
