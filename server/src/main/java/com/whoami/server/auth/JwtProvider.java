package com.whoami.server.auth;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtProvider {
    // Generates a secure random key for HS256 algorithm
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    private static final long ACCESS_TOKEN_VALIDITY = 3600000; // 1 hour

    public static String generateAccessToken(int userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getUserIdFromToken(String token) {
        String subject = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload().getSubject();
        return Integer.parseInt(subject);
    }
}
