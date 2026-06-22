package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.whoami.protocol.models.UserProfile;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected;
    private UserProfile userProfile;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;
        try {
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Failed to get I/O streams: " + e.getMessage());
            this.isConnected = false;
        }
    }

    @Override
    public void run() {
        while (isConnected) {
            try {
                Packet packet = PacketBuilder.readFromStream(in);
                PacketRouter.route(packet, this);
            } catch (IOException e) {
                System.err.println("Client disconnected or error reading packet: " + e.getMessage());
                disconnect();
            }
        }
    }

    public void sendPacket(Packet packet) {
        if (!isConnected) return;
        try {
            byte[] data = PacketBuilder.toBytes(packet);
            out.write(data);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send packet: " + e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        this.isConnected = false;
        try {
            clientSocket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
}
