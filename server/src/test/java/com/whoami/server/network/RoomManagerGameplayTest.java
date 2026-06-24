package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.database.CharacterDAO;
import com.whoami.server.game.GameLogic;
import com.whoami.server.game.GameResults;
import com.whoami.server.game.InMemoryGameResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RoomManagerGameplayTest {

    private RoomManager roomManager;
    private ClientHandler host;
    private ClientHandler guest;
    private MockedStatic<CharacterDAO> mockedCharacterDAO;
    private InMemoryGameResultRepository results;

    private ClientHandler guesser;
    private ClientHandler riddler;

    @BeforeEach
    public void setUp() {
        roomManager = RoomManager.getInstance();
        host = mock(ClientHandler.class);
        guest = mock(ClientHandler.class);
        mockedCharacterDAO = mockStatic(CharacterDAO.class);
        mockedCharacterDAO.when(() -> CharacterDAO.getRandomCharacters(anyInt()))
                .thenReturn(List.of("TestCharacter"));
        results = new InMemoryGameResultRepository();
        GameResults.set(results);

        String code = roomManager.createRoom(host);
        roomManager.joinRoom(code, guest);
        GameRoom room = roomManager.findRoom(host);
        guesser = room.getGuesser();
        riddler = room.getRiddler();
        // New flow: the riddler must pick a character before the round starts.
        roomManager.submitCharacter(riddler, "TestCharacter");
    }

    @AfterEach
    public void tearDown() {
        mockedCharacterDAO.close();
        roomManager.leaveRoom(host);
        roomManager.leaveRoom(guest);
    }

    private boolean received(ClientHandler player, PacketType type) {
        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(player, atLeast(0)).sendPacket(captor.capture());
        return captor.getAllValues().stream().anyMatch(p -> p.getPacketType() == type.getId());
    }

    @Test
    public void questionAndAnswerBroadcastStateToBoth() {
        assertTrue(roomManager.submitQuestion(guesser, "Are you fictional?"));
        assertTrue(roomManager.submitAnswer(riddler, GameLogic.Answer.YES));

        assertTrue(received(guesser, PacketType.GAME_STATE));
        assertTrue(received(riddler, PacketType.GAME_STATE));
    }

    @Test
    public void onlyGuesserMayAskAndOnlyRiddlerMayAnswer() {
        assertFalse(roomManager.submitQuestion(riddler, "illegal"), "riddler cannot ask");
        assertTrue(roomManager.submitQuestion(guesser, "legit?"));
        assertFalse(roomManager.submitAnswer(guesser, GameLogic.Answer.YES), "guesser cannot answer");
    }

    @Test
    public void correctGuessEndsGameAndIsPersisted() {
        roomManager.submitQuestion(guesser, "Hint?");
        roomManager.submitAnswer(riddler, GameLogic.Answer.PARTIALLY);

        assertEquals(GameLogic.GuessResult.CORRECT, roomManager.submitGuess(guesser, "testcharacter"));

        assertTrue(received(guesser, PacketType.GAME_OVER));
        assertTrue(received(riddler, PacketType.GAME_OVER));
        assertEquals(GameRoom.State.FINISHED, roomManager.findRoom(host).getState());

        assertEquals(1, results.all().size());
        assertEquals("TestCharacter", results.all().get(0).characterName());
        assertEquals(1, results.all().get(0).questionsAsked());
    }

    @Test
    public void rematchOnlyAfterRoundFinishedThenRestarts() {
        assertEquals(RoomManager.RematchResult.REJECTED, roomManager.requestRematch(guesser),
                "cannot rematch mid-game");

        assertEquals(GameLogic.GuessResult.CORRECT, roomManager.submitGuess(guesser, "TestCharacter"));

        assertEquals(RoomManager.RematchResult.WAITING, roomManager.requestRematch(guesser));
        assertEquals(RoomManager.RematchResult.RESTARTED, roomManager.requestRematch(riddler));

        // A rematch re-assigns roles and asks the (possibly new) riddler to choose again.
        GameRoom room = roomManager.findRoom(host);
        assertEquals(GameRoom.State.SELECTING, room.getState());
        roomManager.submitCharacter(room.getRiddler(), "TestCharacter");
        assertEquals(GameRoom.State.IN_PROGRESS, room.getState());
    }

    @Test
    public void threeWrongGuessesEndGameForRiddler() {
        assertEquals(GameLogic.GuessResult.WRONG_RETRY, roomManager.submitGuess(guesser, "Nope1"));
        assertEquals(GameLogic.GuessResult.WRONG_RETRY, roomManager.submitGuess(guesser, "Nope2"));
        assertEquals(GameLogic.GuessResult.WRONG_GAME_OVER, roomManager.submitGuess(guesser, "Nope3"));

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(guesser, atLeastOnce()).sendPacket(captor.capture());
        List<Packet> sent = captor.getAllValues();
        boolean riddlerWon = sent.stream().anyMatch(p ->
                p.getPacketType() == PacketType.GAME_OVER.getId()
                        && new String(p.getPayload(), StandardCharsets.UTF_8).contains("WINNER=RIDDLER"));
        assertTrue(riddlerWon);
        assertEquals(1, results.all().size());
    }
}
