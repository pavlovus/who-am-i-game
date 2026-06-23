package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.database.CharacterDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RoomManagerDisconnectTest {

    private RoomManager roomManager;
    private ClientHandler host;
    private ClientHandler guest;
    private MockedStatic<CharacterDAO> mockedCharacterDAO;

    @BeforeEach
    public void setUp() {
        roomManager = RoomManager.getInstance();
        host = mock(ClientHandler.class);
        guest = mock(ClientHandler.class);
        mockedCharacterDAO = mockStatic(CharacterDAO.class);
        mockedCharacterDAO.when(CharacterDAO::getRandomCharacter).thenReturn("TestCharacter");
    }

    @AfterEach
    public void tearDown() {
        mockedCharacterDAO.close();
        roomManager.leaveRoom(host);
        roomManager.leaveRoom(guest);
    }

    @Test
    public void opponentIsNotifiedWhenPlayerLeavesMidGame() {
        String code = roomManager.createRoom(host);
        roomManager.joinRoom(code, guest); // starts game, sends GAME_START to both

        roomManager.leaveRoom(host);

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(guest, atLeastOnce()).sendPacket(captor.capture());

        List<Packet> sent = captor.getAllValues();
        boolean gotGameOver = sent.stream().anyMatch(p ->
                p.getPacketType() == PacketType.GAME_OVER.getId()
                        && "OPPONENT_LEFT".equals(new String(p.getPayload(), StandardCharsets.UTF_8)));
        assertTrue(gotGameOver, "remaining player must receive GAME_OVER:OPPONENT_LEFT");
    }

    @Test
    public void rematchRejectedWhileGameInProgress() {
        String code = roomManager.createRoom(host);
        roomManager.joinRoom(code, guest); // state is IN_PROGRESS

        assertEquals(RoomManager.RematchResult.REJECTED, roomManager.requestRematch(host),
                "rematch must not interrupt an active round");
    }
}
