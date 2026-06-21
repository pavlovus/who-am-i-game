package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;

public class PacketRouter {

    public static void route(Packet packet, ClientHandler handler) {
        try {
            PacketType type = PacketType.fromId(packet.getPacketType());
            System.out.println("Received packet: " + type + " from client: " + packet.getClientId());
            
            // TODO: Route to respective services based on PacketType
            switch (type) {
                case AUTH_REQUEST:
                    System.out.println("Processing AUTH_REQUEST...");
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
