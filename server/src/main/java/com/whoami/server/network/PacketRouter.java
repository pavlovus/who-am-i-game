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
                    String payloadStr = new String(packet.getPayload(), StandardCharsets.UTF_8);
                    String[] parts = payloadStr.split(":"); // "REGISTER:username:password" or "LOGIN:username:password"
                    if (parts.length == 3) {
                        boolean isRegister = parts[0].equals("REGISTER");
                        AuthService.AuthResult result = AuthService.loginOrRegister(parts[1], parts[2], isRegister);
                        
                        String responsePayload = (result.success ? "SUCCESS:" : "ERROR:") + (result.success ? result.token : result.message);
                        byte[] respData = responsePayload.getBytes(StandardCharsets.UTF_8);
                        
                        Packet response = new Packet(Packet.MAGIC_BYTE, packet.getClientId(), PacketType.AUTH_RESPONSE.getId(), respData.length, (short)0, respData);
                        handler.sendPacket(response);
                    } else {
                        System.err.println("Invalid AUTH_REQUEST payload format");
                    }
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
