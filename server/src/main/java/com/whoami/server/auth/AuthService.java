package com.whoami.server.auth;

import com.whoami.protocol.models.UserProfile;
import com.whoami.server.database.UserDAO;

import java.sql.SQLException;

public class AuthService {

    public static AuthResult loginOrRegister(String username, String rawPassword, boolean isRegister) {
        try {
            UserProfile profile;
            if (isRegister) {
                profile = UserDAO.register(username, rawPassword);
            } else {
                profile = UserDAO.login(username, rawPassword);
                if (profile != null && UserDAO.isBlocked(username)) {
                    return new AuthResult(false, "Account is blocked", null, null);
                }
            }

            if (profile != null) {
                String token = JwtProvider.generateAccessToken(profile.getId(), profile.getUsername());
                return new AuthResult(true, "Success", token, profile);
            } else {
                return new AuthResult(false, "Invalid credentials or username already taken", null, null);
            }
        } catch (SQLException e) {
            return new AuthResult(false, "Database error: " + e.getMessage(), null, null);
        }
    }

    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final String token;
        public final UserProfile profile;

        public AuthResult(boolean success, String message, String token, UserProfile profile) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.profile = profile;
        }
    }
}
