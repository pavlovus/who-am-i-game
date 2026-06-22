package com.whoami.server.database;

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

public class CharacterDAOTest {

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

        mockedPool = mockStatic(ConnectionPool.class);
        mockedPool.when(ConnectionPool::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    public void tearDown() {
        mockedPool.close();
    }

    @Test
    public void testGetRandomCharacterSuccess() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("name")).thenReturn("Batman");

        String character = CharacterDAO.getRandomCharacter();

        assertEquals("Batman", character);
        verify(mockStatement).executeQuery();
    }

    @Test
    public void testGetRandomCharacterEmptyDB() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        String character = CharacterDAO.getRandomCharacter();

        assertEquals("Unknown Character", character, "Should return fallback name when DB is empty");
        verify(mockStatement).executeQuery();
    }

    @Test
    public void testGetRandomCharacterSQLException() throws SQLException {
        when(mockStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        String character = CharacterDAO.getRandomCharacter();

        assertEquals("Unknown Character", character, "Should return fallback name when SQLException occurs");
    }
}
