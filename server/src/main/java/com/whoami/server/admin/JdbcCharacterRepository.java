package com.whoami.server.admin;

import com.whoami.server.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** PreparedStatement-only JDBC implementation backed by the {@code characters} table. */
public class JdbcCharacterRepository implements CharacterRepository {

    @Override
    public CharacterRecord add(String name, String category, String status) {
        String sql = "INSERT INTO characters (name, category, status) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CharacterRecord(rs.getInt("id"), name, category, status);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add character: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<CharacterRecord> listAll() {
        String sql = "SELECT id, name, category, status FROM characters ORDER BY id";
        List<CharacterRecord> result = new ArrayList<>();
        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new CharacterRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list characters: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM characters WHERE id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete character: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean setStatus(int id, String status) {
        String sql = "UPDATE characters SET status = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update character: " + e.getMessage(), e);
        }
    }
}
