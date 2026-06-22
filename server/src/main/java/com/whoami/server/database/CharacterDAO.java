package com.whoami.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
            System.err.println("Database error fetching character: " + e.getMessage());
        }
        // Fallback just in case
        return "Unknown Character";
    }
}
