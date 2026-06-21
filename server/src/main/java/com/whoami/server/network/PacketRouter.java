package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.server.auth.AuthService;

import java.nio.charset.StandardCharsets;

public class PacketRouter {

    public static void route(Packet packet, ClientHandler handler) {
        try {
            PacketType type = PacketType.fromId(packet.getPacketType());
            System.out.println("Received packet: " + type + " from client: " + packet.getClientId());
            
            switch (type) {
                case AUTH_REQUEST:
                    System.out.println("Processing AUTH_REQUEST...");
                    // Placeholder for Auth parsing.
                    // E.g. String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
                    // String[] parts = payload.split(":"); // "REGISTER:username:password"
                    // AuthService.AuthResult result = AuthService.loginOrRegister(parts[1], parts[2], parts[0].equals("REGISTER"));
                    // handler.sendPacket(new Packet(Packet.MAGIC_BYTE, packet.getClientId(), PacketType.AUTH_RESPONSE.getId(), ...));
                    break;
                case ROOM_CREATE:
                    System.out.println("Processing ROOM_CREATE...");
                    break;
                default:
                    System.out.println("Unhandled packet type: " + type);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to route packet: " + e.getMessage());
        }
    }
}
