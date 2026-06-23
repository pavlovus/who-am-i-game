package com.whoami.server.game;

import com.whoami.protocol.util.Log;
import com.whoami.server.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Persists the round into {@code games} and bumps player statistics. Best-effort:
 * a DB failure is logged but never breaks the game thread. One row is written per
 * finished round (the UNIQUE constraint on room_code is dropped in V3) so rematches
 * stay consistent with the stat increments.
 */
public class JdbcGameResultRepository implements GameResultRepository {

    @Override
    public void record(GameResult result) {
        try (Connection conn = ConnectionPool.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertGame(conn, result);
                bumpGamesPlayed(conn, result.riddlerUserId());
                bumpGamesPlayed(conn, result.guesserUserId());
                if (result.winnerUserId() != null) {
                    bumpGamesWon(conn, result.winnerUserId());
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            Log.error("Failed to persist game result", e);
        }
    }

    private void insertGame(Connection conn, GameResult result) throws SQLException {
        String sql = "INSERT INTO games (room_code, player1_id, player2_id, winner_id, total_questions, status) "
                + "VALUES (?, ?, ?, ?, ?, 'finished')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.roomCode());
            setNullableInt(stmt, 2, result.riddlerUserId());
            setNullableInt(stmt, 3, result.guesserUserId());
            setNullableInt(stmt, 4, result.winnerUserId());
            stmt.setInt(5, result.questionsAsked());
            stmt.executeUpdate();
        }
    }

    private void bumpGamesPlayed(Connection conn, Integer userId) throws SQLException {
        if (userId == null) {
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET games_played = games_played + 1 WHERE id = ?")) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private void bumpGamesWon(Connection conn, Integer userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET games_won = games_won + 1 WHERE id = ?")) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }
}
