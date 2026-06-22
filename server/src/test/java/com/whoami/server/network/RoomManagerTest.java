package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.database.CharacterDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RoomManagerTest {

    private RoomManager roomManager;
    private ClientHandler mockHost;
    private ClientHandler mockGuest;
    private MockedStatic<CharacterDAO> mockedCharacterDAO;

    @BeforeEach
    public void setUp() {
        roomManager = RoomManager.getInstance();
        mockHost = mock(ClientHandler.class);
        mockGuest = mock(ClientHandler.class);

        // Mock CharacterDAO to avoid DB calls during GAME_START
        mockedCharacterDAO = mockStatic(CharacterDAO.class);
        mockedCharacterDAO.when(CharacterDAO::getRandomCharacter).thenReturn("TestCharacter");
    }

    @AfterEach
    public void tearDown() {
        // Clean up mock static
        mockedCharacterDAO.close();

        // Ensure we leave the room manager in a clean state for the host/guest
        roomManager.leaveRoom(mockHost);
        roomManager.leaveRoom(mockGuest);
    }

    @Test
    public void testCreateRoom() {
        String roomCode = roomManager.createRoom(mockHost);

        assertNotNull(roomCode);
        assertEquals(6, roomCode.length());
        
        // Cannot join an invalid room
        assertFalse(roomManager.joinRoom("INVALID", mockGuest));
    }

    @Test
    public void testJoinRoomSuccessAndGameStart() {
        String roomCode = roomManager.createRoom(mockHost);
        
        boolean joined = roomManager.joinRoom(roomCode, mockGuest);
        
        assertTrue(joined, "Guest should successfully join a newly created room");

        // Verify GAME_START packet is sent to both players
        verify(mockHost, times(1)).sendPacket(any(Packet.class));
        verify(mockGuest, times(1)).sendPacket(any(Packet.class));
    }

    @Test
    public void testJoinRoomFull() {
        String roomCode = roomManager.createRoom(mockHost);
        roomManager.joinRoom(roomCode, mockGuest); // Room is now full

        ClientHandler mockExtraPlayer = mock(ClientHandler.class);
        boolean joined = roomManager.joinRoom(roomCode, mockExtraPlayer);
        
        assertFalse(joined, "Third player should not be able to join a full room");
    }

    @Test
    public void testLeaveRoom() {
        String roomCode = roomManager.createRoom(mockHost);
        
        roomManager.leaveRoom(mockHost);
        
        boolean joined = roomManager.joinRoom(roomCode, mockGuest);
        
        assertFalse(joined, "Guest should not be able to join after the host leaves the room (room should be destroyed)");
    }
}
