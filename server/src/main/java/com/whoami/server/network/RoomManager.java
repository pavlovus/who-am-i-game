package com.whoami.server.network;

import com.whoami.protocol.crypto.AESEncrypter;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.protocol.util.Log;
import com.whoami.server.database.CharacterDAO;
import com.whoami.server.game.GameLogic;
import com.whoami.server.game.GameResult;
import com.whoami.server.game.GameResults;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final RoomManager instance = new RoomManager();
    private final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final AESEncrypter encrypter = new AESEncrypter();

    private RoomManager() {}

    public static RoomManager getInstance() {
        return instance;
    }

    public synchronized String createRoom(ClientHandler host) {
        String roomCode;
        do {
            roomCode = generateRoomCode();
        } while (activeRooms.containsKey(roomCode));

        GameRoom room = new GameRoom(roomCode, host);
        activeRooms.put(roomCode, room);
        return roomCode;
    }

    public synchronized boolean joinRoom(String roomCode, ClientHandler guest) {
        GameRoom room = activeRooms.get(roomCode);
        if (room != null && !room.isFull()) {
            room.setPlayer2(guest);
            startGame(room);
            return true;
        }
        return false;
    }

    public synchronized void leaveRoom(ClientHandler client) {
        GameRoom room = findRoom(client);
        if (room == null) {
            return;
        }
        ClientHandler opponent = room.getOpponent(client);
        if (opponent != null && room.getState() == GameRoom.State.IN_PROGRESS) {
            sendInfo(opponent, PacketType.GAME_OVER, "OPPONENT_LEFT");
        }
        activeRooms.remove(room.getRoomCode());
        Log.info("Client left room: " + room.getRoomCode());
    }

    public enum RematchResult { RESTARTED, WAITING, REJECTED }

    /** A rematch is only valid once the previous round has finished. */
    public synchronized RematchResult requestRematch(ClientHandler client) {
        GameRoom room = findRoom(client);
        if (room == null || !room.isFull() || room.getState() != GameRoom.State.FINISHED) {
            return RematchResult.REJECTED;
        }
        boolean bothReady = room.requestRematch(client);
        if (bothReady) {
            room.resetForRematch();
            startGame(room);
            return RematchResult.RESTARTED;
        }
        return RematchResult.WAITING;
    }

    /** Guesser asks a question; both players receive the refreshed GAME_STATE. */
    public synchronized boolean submitQuestion(ClientHandler sender, String question) {
        GameRoom room = playableRoom(sender);
        if (room == null || sender != room.getGuesser()) {
            return false;
        }
        if (!room.getLogic().submitQuestion(question)) {
            return false;
        }
        broadcastState(room);
        return true;
    }

    /** Riddler answers; broadcasts GAME_STATE, then ends the round if questions ran out. */
    public synchronized boolean submitAnswer(ClientHandler sender, GameLogic.Answer answer) {
        GameRoom room = playableRoom(sender);
        if (room == null || sender != room.getRiddler()) {
            return false;
        }
        if (!room.getLogic().submitAnswer(answer)) {
            return false;
        }
        broadcastState(room);
        if (room.getLogic().isFinished()) {
            finishGame(room);
        }
        return true;
    }

    /** Guesser's final attempt. Ends the round on a correct guess or last failure. */
    public synchronized GameLogic.GuessResult submitGuess(ClientHandler sender, String guess) {
        GameRoom room = playableRoom(sender);
        if (room == null || sender != room.getGuesser()) {
            return GameLogic.GuessResult.REJECTED;
        }
        GameLogic.GuessResult result = room.getLogic().submitGuess(guess);
        if (room.getLogic().isFinished()) {
            finishGame(room);
        } else if (result == GameLogic.GuessResult.WRONG_RETRY) {
            broadcastState(room);
        }
        return result;
    }

    private GameRoom playableRoom(ClientHandler client) {
        GameRoom room = findRoom(client);
        if (room == null || room.getLogic() == null || room.getState() != GameRoom.State.IN_PROGRESS) {
            return null;
        }
        return room;
    }

    private void broadcastState(GameRoom room) {
        GameLogic logic = room.getLogic();
        String payload = "Q_LEFT=" + logic.getRemainingQuestions()
                + ";G_LEFT=" + logic.getRemainingGuesses()
                + ";PENDING=" + (logic.getPendingQuestion() == null ? "" : logic.getPendingQuestion())
                + ";LAST_Q=" + (logic.getLastQuestion() == null ? "" : logic.getLastQuestion())
                + ";LAST_A=" + (logic.getLastAnswer() == null ? "" : logic.getLastAnswer());
        sendInfo(room.getRiddler(), PacketType.GAME_STATE, payload);
        sendInfo(room.getGuesser(), PacketType.GAME_STATE, payload);
    }

    private void finishGame(GameRoom room) {
        GameLogic logic = room.getLogic();
        room.setState(GameRoom.State.FINISHED);

        String winnerRole = logic.getWinner() == GameLogic.Winner.GUESSER ? "GUESSER" : "RIDDLER";
        String payload = "WINNER=" + winnerRole
                + ";CHARACTER=" + room.getCharacterName()
                + ";QUESTIONS=" + logic.getQuestionsAsked();
        sendInfo(room.getRiddler(), PacketType.GAME_OVER, payload);
        sendInfo(room.getGuesser(), PacketType.GAME_OVER, payload);

        persist(room, logic);
    }

    private void persist(GameRoom room, GameLogic logic) {
        Integer riddlerId = userId(room.getRiddler());
        Integer guesserId = userId(room.getGuesser());
        Integer winnerId = logic.getWinner() == GameLogic.Winner.GUESSER ? guesserId : riddlerId;
        try {
            GameResults.get().record(new GameResult(room.getRoomCode(), riddlerId, guesserId,
                    winnerId, room.getCharacterName(), logic.getQuestionsAsked()));
        } catch (RuntimeException e) {
            Log.error("Failed to record game result", e);
        }
    }

    private Integer userId(ClientHandler handler) {
        return handler != null ? handler.getUserId() : null;
    }

    GameRoom findRoom(ClientHandler client) {
        for (GameRoom room : activeRooms.values()) {
            if (room.contains(client)) {
                return room;
            }
        }
        return null;
    }

    int activeRoomCount() {
        return activeRooms.size();
    }

    private void startGame(GameRoom room) {
        boolean hostIsRiddler = random.nextBoolean();
        if (hostIsRiddler) {
            room.setRiddler(room.getPlayer1());
            room.setGuesser(room.getPlayer2());
        } else {
            room.setRiddler(room.getPlayer2());
            room.setGuesser(room.getPlayer1());
        }

        String characterName = CharacterDAO.getRandomCharacter();
        room.setCharacterName(characterName);
        room.setLogic(new GameLogic(characterName));
        room.setState(GameRoom.State.IN_PROGRESS);

        byte[] encryptedCharBytes = encrypter.encrypt(characterName.getBytes(StandardCharsets.UTF_8));
        String encryptedCharHex = bytesToHex(encryptedCharBytes);

        Log.info("Starting game in room " + room.getRoomCode() + " with character: " + characterName);

        sendStartPacket(room.getRiddler(), "ROLE:Riddler:" + encryptedCharHex);
        sendStartPacket(room.getGuesser(), "ROLE:Guesser:" + encryptedCharHex);
    }

    private void sendStartPacket(ClientHandler client, String payloadString) {
        byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.GAME_START.getId(), payload.length, (short)0, payload);
        client.sendPacket(packet);
    }

    private void sendInfo(ClientHandler client, PacketType type, String message) {
        if (client == null) {
            return;
        }
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        client.sendPacket(new Packet(Packet.MAGIC_BYTE, 0, type.getId(), payload.length, (short)0, payload));
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
