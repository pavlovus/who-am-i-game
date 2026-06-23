package com.whoami.server.auth;

import com.whoami.protocol.models.UserProfile;
import com.whoami.server.database.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private MockedStatic<UserDAO> mockedUserDAO;

    @BeforeEach
    public void setUp() {
        // Mock static methods of UserDAO to avoid real DB connections
        mockedUserDAO = Mockito.mockStatic(UserDAO.class);
    }

    @AfterEach
    public void tearDown() {
        mockedUserDAO.close();
    }

    @Test
    public void testSuccessfulLogin() throws SQLException {
        UserProfile mockProfile = new UserProfile(1, "testuser", 5, 2);
        mockedUserDAO.when(() -> UserDAO.login("testuser", "correctPass")).thenReturn(mockProfile);

        AuthService.AuthResult result = AuthService.loginOrRegister("testuser", "correctPass", false);

        assertTrue(result.success, "Login should be successful");
        assertEquals("Success", result.message);
        assertNotNull(result.token, "JWT token must be generated on successful login");
        assertEquals(mockProfile, result.profile);
    }

    @Test
    public void testFailedLogin() throws SQLException {
        mockedUserDAO.when(() -> UserDAO.login("testuser", "wrongPass")).thenReturn(null);

        AuthService.AuthResult result = AuthService.loginOrRegister("testuser", "wrongPass", false);

        assertFalse(result.success, "Login should fail with wrong password");
        assertNull(result.token, "No token should be generated on failed login");
        assertNull(result.profile);
    }

    @Test
    public void testSuccessfulRegistration() throws SQLException {
        UserProfile mockProfile = new UserProfile(2, "newuser", 0, 0);
        mockedUserDAO.when(() -> UserDAO.register("newuser", "newPass")).thenReturn(mockProfile);

        AuthService.AuthResult result = AuthService.loginOrRegister("newuser", "newPass", true);

        assertTrue(result.success, "Registration should be successful");
        assertNotNull(result.token);
        assertEquals(mockProfile, result.profile);
    }

    @Test
    public void testBlockedUserCannotLogin() throws SQLException {
        UserProfile mockProfile = new UserProfile(7, "banned", 1, 0);
        mockedUserDAO.when(() -> UserDAO.login("banned", "pass")).thenReturn(mockProfile);
        mockedUserDAO.when(() -> UserDAO.isBlocked("banned")).thenReturn(true);

        AuthService.AuthResult result = AuthService.loginOrRegister("banned", "pass", false);

        assertFalse(result.success, "Blocked users must not be able to log in");
        assertNull(result.token);
        assertEquals("Account is blocked", result.message);
    }

    @Test
    public void testDatabaseErrorDuringAuth() throws SQLException {
        mockedUserDAO.when(() -> UserDAO.login("testuser", "pass")).thenThrow(new SQLException("DB Down"));

        AuthService.AuthResult result = AuthService.loginOrRegister("testuser", "pass", false);

        assertFalse(result.success);
        assertTrue(result.message.contains("Database error"));
        assertNull(result.token);
    }
}
