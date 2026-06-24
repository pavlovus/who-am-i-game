package com.whoami.server.database;

import com.whoami.protocol.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CharacterDAO {

    /**
     * Gets a random approved character from the database.
     */
    public static String getRandomCharacter() {
        String query = "SELECT name FROM characters WHERE status = 'approved' ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            Log.error("Database error fetching character", e);
        }
        // Fallback just in case
        return "Unknown Character";
    }

    /**
     * Returns up to {@code limit} random approved characters, used to offer the
     * riddler a "pick from the bank" shortlist alongside typing a custom one.
     */
    public static List<String> getRandomCharacters(int limit) {
        List<String> names = new ArrayList<>();
        String query = "SELECT name FROM characters WHERE status = 'approved' ORDER BY RANDOM() LIMIT ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            Log.error("Database error fetching characters", e);
        }
        return names;
    }
}
