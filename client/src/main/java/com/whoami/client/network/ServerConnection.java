package com.whoami.client.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected;
    private PacketListener listener;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setPacketListener(PacketListener listener) {
        this.listener = listener;
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            isConnected = true;
            
            System.out.println("Connected to server at " + host + ":" + port);
            
            new Thread(this::listenForPackets).start();
            
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            isConnected = false;
        }
    }

    private void listenForPackets() {
        while (isConnected) {
            try {
                Packet packet = PacketBuilder.readFromStream(in);
                System.out.println("Received packet of type: " + packet.getPacketType());
                if (listener != null) {
                    listener.onPacketReceived(packet);
                }
            } catch (IOException e) {
                System.err.println("Disconnected from server: " + e.getMessage());
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
        if (!isConnected) return;
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        if (listener != null) {
            listener.onDisconnected();
        }
    }
}
