package com.whoami.server.database;

import com.whoami.protocol.models.UserProfile;
import com.whoami.server.auth.PasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public static UserProfile register(String username, String rawPassword) throws SQLException {
        String query = "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING id, games_played, games_won";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, PasswordEncoder.encode(rawPassword));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserProfile(
                            rs.getInt("id"),
                            username,
                            rs.getInt("games_played"),
                            rs.getInt("games_won")
                    );
                }
            }
        }
        return null;
    }

    public static UserProfile login(String username, String rawPassword) throws SQLException {
        String query = "SELECT id, password_hash, games_played, games_won FROM users WHERE username = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (PasswordEncoder.matches(rawPassword, hash)) {
                        return new UserProfile(
                                rs.getInt("id"),
                                username,
                                rs.getInt("games_played"),
                                rs.getInt("games_won")
                        );
                    }
                }
            }
        }
        return null; // Login failed
    }
}
