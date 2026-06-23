package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.protocol.util.Log;
import com.whoami.protocol.validation.CredentialsValidator;
import com.whoami.protocol.validation.ValidationResult;
import com.whoami.server.admin.AdminServices;
import com.whoami.server.admin.CharacterRecord;
import com.whoami.server.admin.ConnectionInfo;
import com.whoami.server.admin.SystemStats;
import com.whoami.server.auth.AuthService;
import com.whoami.server.database.UserDAO;
import com.whoami.server.game.GameLogic;

import java.nio.charset.StandardCharsets;

public class PacketRouter {

    public static void route(Packet packet, ClientHandler handler) {
        try {
            PacketType type = PacketType.fromId(packet.getPacketType());
            Log.info("Received packet: " + type + " from client: " + packet.getClientId());

            switch (type) {
                case AUTH_REQUEST:
                    handleAuth(packet, handler);
                    break;
                case ROOM_CREATE:
                    String newRoomCode = RoomManager.getInstance().createRoom(handler);
                    respond(handler, packet, PacketType.ROOM_CREATE, "SUCCESS:" + newRoomCode);
                    break;
                case ROOM_JOIN:
                    String joinCode = new String(packet.getPayload(), StandardCharsets.UTF_8);
                    boolean joined = RoomManager.getInstance().joinRoom(joinCode, handler);
                    respond(handler, packet, PacketType.ROOM_JOIN, joined ? "SUCCESS" : "ERROR:Room full or not found");
                    break;
                case ROOM_LEAVE:
                    RoomManager.getInstance().leaveRoom(handler);
                    respond(handler, packet, PacketType.ROOM_LEAVE, "SUCCESS");
                    break;
                case QUESTION:
                    handleQuestion(packet, handler);
                    break;
                case ANSWER:
                    handleAnswer(packet, handler);
                    break;
                case GUESS:
                    handleGuess(packet, handler);
                    break;
                case REMATCH:
                    handleRematch(packet, handler);
                    break;
                case ADMIN_LIST:
                    handleAdminList(packet, handler);
                    break;
                case ADMIN_KICK:
                    handleAdminKick(packet, handler);
                    break;
                case ADMIN_BLOCK:
                    handleAdminBlock(packet, handler);
                    break;
                case ADMIN_ADD_CHARACTER:
                    handleAdminAddCharacter(packet, handler);
                    break;
                case ADMIN_STATS:
                    handleAdminStats(packet, handler);
                    break;
                default:
                    Log.info("Unhandled packet type: " + type);
            }
        } catch (IllegalArgumentException e) {
            Log.error("Failed to route packet", e);
        }
    }

    private static void handleAuth(Packet packet, ClientHandler handler) {
        String payloadStr = new String(packet.getPayload(), StandardCharsets.UTF_8);
        String[] parts = payloadStr.split(":");
        if (parts.length != 3) {
            Log.error("Invalid AUTH_REQUEST payload format");
            return;
        }
        String username = parts[1];
        String password = parts[2];

        ValidationResult validation = CredentialsValidator.validateCredentials(username, password);
        if (!validation.isValid()) {
            respond(handler, packet, PacketType.AUTH_RESPONSE, "ERROR:" + validation.getMessage());
            return;
        }

        boolean isRegister = parts[0].equals("REGISTER");
        AuthService.AuthResult result = AuthService.loginOrRegister(username, password, isRegister);

        if (result.success) {
            handler.setUserProfile(result.profile);
            handler.setAdmin(resolveAdmin(username));
        }

        String responsePayload = (result.success ? "SUCCESS:" + result.token : "ERROR:" + result.message);
        respond(handler, packet, PacketType.AUTH_RESPONSE, responsePayload);
    }

    private static boolean resolveAdmin(String username) {
        try {
            return UserDAO.isAdmin(username);
        } catch (Exception e) {
            Log.error("Failed to resolve admin role", e);
            return false;
        }
    }

    private static void handleRematch(Packet packet, ClientHandler handler) {
        RoomManager.RematchResult result = RoomManager.getInstance().requestRematch(handler);
        switch (result) {
            case RESTARTED -> respond(handler, packet, PacketType.REMATCH, "RESTARTED");
            case WAITING -> respond(handler, packet, PacketType.REMATCH, "WAITING");
            case REJECTED -> respond(handler, packet, PacketType.ERROR, "REMATCH_REJECTED");
        }
    }

    private static void handleQuestion(Packet packet, ClientHandler handler) {
        String text = new String(packet.getPayload(), StandardCharsets.UTF_8);
        if (!RoomManager.getInstance().submitQuestion(handler, text)) {
            respond(handler, packet, PacketType.ERROR, "QUESTION_REJECTED");
        }
    }

    private static void handleAnswer(Packet packet, ClientHandler handler) {
        String raw = new String(packet.getPayload(), StandardCharsets.UTF_8).trim().toUpperCase();
        GameLogic.Answer answer;
        try {
            answer = GameLogic.Answer.valueOf(raw);
        } catch (IllegalArgumentException e) {
            respond(handler, packet, PacketType.ERROR, "BAD_ANSWER");
            return;
        }
        if (!RoomManager.getInstance().submitAnswer(handler, answer)) {
            respond(handler, packet, PacketType.ERROR, "ANSWER_REJECTED");
        }
    }

    private static void handleGuess(Packet packet, ClientHandler handler) {
        String name = new String(packet.getPayload(), StandardCharsets.UTF_8);
        GameLogic.GuessResult result = RoomManager.getInstance().submitGuess(handler, name);
        if (result == GameLogic.GuessResult.REJECTED) {
            respond(handler, packet, PacketType.ERROR, "GUESS_REJECTED");
        }
    }

    private static void handleAdminList(Packet packet, ClientHandler handler) {
        if (denyIfNotAdmin(packet, handler)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ConnectionInfo info : AdminServices.get().listConnections()) {
            sb.append(info.sessionId()).append('|')
              .append(info.username() == null ? "-" : info.username()).append('|')
              .append(info.authenticated()).append('\n');
        }
        respond(handler, packet, PacketType.ADMIN_LIST, sb.toString());
    }

    private static void handleAdminKick(Packet packet, ClientHandler handler) {
        if (denyIfNotAdmin(packet, handler)) {
            return;
        }
        int sessionId = parseIntOrNeg(new String(packet.getPayload(), StandardCharsets.UTF_8));
        boolean kicked = AdminServices.get().kickConnection(sessionId);
        respond(handler, packet, PacketType.ADMIN_KICK, kicked ? "SUCCESS" : "ERROR:Not found");
    }

    private static void handleAdminBlock(Packet packet, ClientHandler handler) {
        if (denyIfNotAdmin(packet, handler)) {
            return;
        }
        int userId = parseIntOrNeg(new String(packet.getPayload(), StandardCharsets.UTF_8));
        boolean blocked = AdminServices.get().blockUser(userId);
        respond(handler, packet, PacketType.ADMIN_BLOCK, blocked ? "SUCCESS" : "ERROR:Not found");
    }

    private static void handleAdminAddCharacter(Packet packet, ClientHandler handler) {
        if (denyIfNotAdmin(packet, handler)) {
            return;
        }
        String[] parts = new String(packet.getPayload(), StandardCharsets.UTF_8).split(":", 2);
        if (parts.length != 2) {
            respond(handler, packet, PacketType.ADMIN_ADD_CHARACTER, "ERROR:Expected name:category");
            return;
        }
        CharacterRecord record = AdminServices.get().addCharacter(parts[0], parts[1]);
        respond(handler, packet, PacketType.ADMIN_ADD_CHARACTER, "SUCCESS:" + record.id());
    }

    private static void handleAdminStats(Packet packet, ClientHandler handler) {
        if (denyIfNotAdmin(packet, handler)) {
            return;
        }
        SystemStats stats = AdminServices.get().systemStats();
        String payload = "connections=" + stats.activeConnections()
                + ";users=" + stats.totalUsers()
                + ";blocked=" + stats.blockedUsers()
                + ";characters=" + stats.totalCharacters()
                + ";approved=" + stats.approvedCharacters();
        respond(handler, packet, PacketType.ADMIN_STATS, payload);
    }

    private static boolean denyIfNotAdmin(Packet packet, ClientHandler handler) {
        if (!handler.isAdmin()) {
            respond(handler, packet, PacketType.ERROR, "FORBIDDEN");
            return true;
        }
        return false;
    }

    private static int parseIntOrNeg(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void respond(ClientHandler handler, Packet request, PacketType type, String payloadString) {
        byte[] data = payloadString.getBytes(StandardCharsets.UTF_8);
        handler.sendPacket(new Packet(Packet.MAGIC_BYTE, request.getClientId(), type.getId(), data.length, (short) 0, data));
    }
}
