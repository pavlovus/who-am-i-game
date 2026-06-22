package com.whoami.server.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameRoomTest {

    private ClientHandler mockHost;
    private ClientHandler mockGuest;
    private GameRoom room;

    @BeforeEach
    public void setUp() {
        mockHost = mock(ClientHandler.class);
        mockGuest = mock(ClientHandler.class);
        room = new GameRoom("TEST01", mockHost);
    }

    @Test
    public void testGameRoomCreation() {
        assertEquals("TEST01", room.getRoomCode());
        assertEquals(mockHost, room.getPlayer1());
        assertNull(room.getPlayer2());
        assertFalse(room.isFull(), "Room should not be full with only 1 player");
    }

    @Test
    public void testPlayerJoin() {
        room.setPlayer2(mockGuest);
        
        assertEquals(mockGuest, room.getPlayer2());
        assertTrue(room.isFull(), "Room should be full after player2 joins");
    }

    @Test
    public void testSetRoles() {
        room.setPlayer2(mockGuest);
        
        room.setRiddler(mockHost);
        room.setGuesser(mockGuest);
        
        assertEquals(mockHost, room.getRiddler());
        assertEquals(mockGuest, room.getGuesser());
    }

    @Test
    public void testSetCharacterName() {
        assertNull(room.getCharacterName());
        
        room.setCharacterName("Sherlock Holmes");
        
        assertEquals("Sherlock Holmes", room.getCharacterName());
    }
}
