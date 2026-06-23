package com.whoami.server.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class GameRoomRematchTest {

    private ClientHandler host;
    private ClientHandler guest;
    private GameRoom room;

    @BeforeEach
    public void setUp() {
        host = mock(ClientHandler.class);
        guest = mock(ClientHandler.class);
        room = new GameRoom("RM0001", host);
        room.setPlayer2(guest);
    }

    @Test
    public void rematchRequiresBothPlayers() {
        assertFalse(room.requestRematch(host), "one vote is not enough");
        assertTrue(room.requestRematch(guest), "second vote completes the rematch");
    }

    @Test
    public void unknownPlayerCannotVote() {
        ClientHandler stranger = mock(ClientHandler.class);
        assertFalse(room.requestRematch(stranger));
    }

    @Test
    public void resetClearsVotesAndState() {
        room.setState(GameRoom.State.FINISHED);
        room.setCharacterName("Yoda");
        room.requestRematch(host);

        room.resetForRematch();

        assertEquals(GameRoom.State.WAITING, room.getState());
        assertNull(room.getCharacterName());
        assertFalse(room.requestRematch(host), "votes must be cleared after reset");
    }

    @Test
    public void opponentLookupWorksBothWays() {
        assertEquals(guest, room.getOpponent(host));
        assertEquals(host, room.getOpponent(guest));
        assertNull(room.getOpponent(mock(ClientHandler.class)));
    }
}
