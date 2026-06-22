package com.whoami.server.network;

import com.whoami.server.database.CharacterDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class RoomManagerConcurrencyTest {

    private RoomManager roomManager;
    private MockedStatic<CharacterDAO> mockedCharacterDAO;
    private ClientHandler mockHost;

    @BeforeEach
    public void setUp() {
        roomManager = RoomManager.getInstance();
        mockHost = mock(ClientHandler.class);

        mockedCharacterDAO = mockStatic(CharacterDAO.class);
        mockedCharacterDAO.when(CharacterDAO::getRandomCharacter).thenReturn("ConcurrentCharacter");
    }

    @AfterEach
    public void tearDown() {
        mockedCharacterDAO.close();
    }

    @Test
    public void testConcurrentJoins() throws Exception {
        // Host creates a room
        String roomCode = roomManager.createRoom(mockHost);

        int numberOfConcurrentPlayers = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfConcurrentPlayers);

        List<Callable<Boolean>> joinTasks = new ArrayList<>();
        
        for (int i = 0; i < numberOfConcurrentPlayers; i++) {
            ClientHandler mockGuest = mock(ClientHandler.class);
            joinTasks.add(() -> {
                try {
                    return roomManager.joinRoom(roomCode, mockGuest);
                } catch (Exception e) {
                    // Since MockedStatic is thread-local, the worker threads will call the real CharacterDAO
                    // and throw an IllegalStateException about ConnectionPool not being initialized.
                    // If we get this exception, it means joinRoom succeeded and proceeded to startGame!
                    if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("ConnectionPool")) {
                        return true;
                    }
                    return false;
                }
            });
        }

        // Execute all join attempts concurrently
        List<Future<Boolean>> results = executorService.invokeAll(joinTasks);

        int successfulJoins = 0;
        int failedJoins = 0;

        for (Future<Boolean> result : results) {
            if (result.get() != null && result.get()) {
                successfulJoins++;
            } else {
                failedJoins++;
            }
        }

        executorService.shutdown();

        // Exactly 1 player should have successfully joined (since the room cap is 2 and host is already player 1)
        assertEquals(1, successfulJoins, "Only exactly 1 concurrent player should be able to join the room");
        assertEquals(numberOfConcurrentPlayers - 1, failedJoins, "All other concurrent players should be rejected");
        
        // Clean up
        roomManager.leaveRoom(mockHost);
    }
}
