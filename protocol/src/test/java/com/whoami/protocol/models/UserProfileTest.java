package com.whoami.protocol.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserProfileTest {

    @Test
    public void testUserProfileCreation() {
        UserProfile profile = new UserProfile(1, "test_user", 10, 5);

        assertEquals(1, profile.getId());
        assertEquals("test_user", profile.getUsername());
        assertEquals(10, profile.getGamesPlayed());
        assertEquals(5, profile.getGamesWon());
    }

    @Test
    public void testToString() {
        UserProfile profile = new UserProfile(2, "player2", 0, 0);
        String str = profile.toString();

        assertTrue(str.contains("id=2"));
        assertTrue(str.contains("username='player2'"));
        assertTrue(str.contains("gamesPlayed=0"));
        assertTrue(str.contains("gamesWon=0"));
    }
}
