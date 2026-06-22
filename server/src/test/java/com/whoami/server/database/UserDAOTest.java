package com.whoami.server.database;

import com.whoami.protocol.models.UserProfile;
import com.whoami.server.auth.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserDAOTest {

    private MockedStatic<ConnectionPool> mockedPool;
    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        // Mock ConnectionPool
        mockedPool = mockStatic(ConnectionPool.class);
        mockedPool.when(ConnectionPool::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    public void tearDown() {
        mockedPool.close();
    }

    @Test
    public void testRegisterSuccess() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(10);
        when(mockResultSet.getInt("games_played")).thenReturn(0);
        when(mockResultSet.getInt("games_won")).thenReturn(0);

        UserProfile profile = UserDAO.register("testuser", "password");

        assertNotNull(profile);
        assertEquals(10, profile.getId());
        assertEquals("testuser", profile.getUsername());

        verify(mockStatement).setString(eq(1), eq("testuser"));
        verify(mockStatement).setString(eq(2), anyString()); // password hash
        verify(mockStatement).executeQuery();
    }

    @Test
    public void testLoginSuccess() throws SQLException {
        String rawPassword = "mypassword";
        String encodedPassword = PasswordEncoder.encode(rawPassword);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(20);
        when(mockResultSet.getString("password_hash")).thenReturn(encodedPassword);
        when(mockResultSet.getInt("games_played")).thenReturn(5);
        when(mockResultSet.getInt("games_won")).thenReturn(2);

        UserProfile profile = UserDAO.login("gamer", rawPassword);

        assertNotNull(profile, "Should return UserProfile on successful login");
        assertEquals(20, profile.getId());
        assertEquals(5, profile.getGamesPlayed());
    }

    @Test
    public void testLoginWrongPassword() throws SQLException {
        String wrongPassword = "wrong";
        String encodedPassword = PasswordEncoder.encode("correct");

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("password_hash")).thenReturn(encodedPassword);

        UserProfile profile = UserDAO.login("gamer", wrongPassword);

        assertNull(profile, "Should return null for wrong password");
    }

    @Test
    public void testLoginUserNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        UserProfile profile = UserDAO.login("unknown", "pass");

        assertNull(profile, "Should return null if user is not found in DB");
    }
}
