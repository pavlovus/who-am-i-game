package com.whoami.server.network;

import com.whoami.protocol.crypto.AESEncrypter;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.database.CharacterDAO;

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
        // Find if this client is in any active room and remove them
        activeRooms.entrySet().removeIf(entry -> {
            GameRoom room = entry.getValue();
            if (room.getPlayer1() == client || room.getPlayer2() == client) {
                // If the game hasn't fully started or someone leaves in waiting state, destroy the room
                System.out.println("Client left room: " + room.getRoomCode());
                return true; // remove the room entirely
            }
            return false;
        });
    }

    private void startGame(GameRoom room) {
        // Randomly assign roles
        boolean hostIsRiddler = random.nextBoolean();
        if (hostIsRiddler) {
            room.setRiddler(room.getPlayer1());
            room.setGuesser(room.getPlayer2());
        } else {
            room.setRiddler(room.getPlayer2());
            room.setGuesser(room.getPlayer1());
        }

        // Get character
        String characterName = CharacterDAO.getRandomCharacter();
        room.setCharacterName(characterName);

        // Encrypt character name
        byte[] encryptedCharBytes = encrypter.encrypt(characterName.getBytes(StandardCharsets.UTF_8));
        String encryptedCharHex = bytesToHex(encryptedCharBytes); // Convert to Hex to send as string safely
        
        System.out.println("Starting game in room " + room.getRoomCode() + " with character: " + characterName);

        // Send GAME_START to Riddler
        String riddlerPayload = "ROLE:Riddler:" + encryptedCharHex;
        sendStartPacket(room.getRiddler(), riddlerPayload);

        // Send GAME_START to Guesser
        String guesserPayload = "ROLE:Guesser:" + encryptedCharHex;
        sendStartPacket(room.getGuesser(), guesserPayload);
    }

    private void sendStartPacket(ClientHandler client, String payloadString) {
        byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.GAME_START.getId(), payload.length, (short)0, payload);
        client.sendPacket(packet);
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
