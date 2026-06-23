package com.whoami.server.admin;

import com.whoami.server.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcUserAdminRepository implements UserAdminRepository {

    @Override
    public List<UserSummary> listAll() {
        String sql = "SELECT id, username, games_played, games_won, is_blocked FROM users ORDER BY id";
        List<UserSummary> result = new ArrayList<>();
        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new UserSummary(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getInt("games_played"),
                        rs.getInt("games_won"),
                        rs.getBoolean("is_blocked")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list users: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean setBlocked(int userId, boolean blocked) {
        String sql = "UPDATE users SET is_blocked = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, blocked);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isBlocked(String username) {
        String sql = "SELECT is_blocked FROM users WHERE username = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("is_blocked");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read user: " + e.getMessage(), e);
        }
    }
}
