package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.protocol.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.whoami.protocol.models.UserProfile;
import com.whoami.server.session.ClientSession;
import com.whoami.server.session.SessionRegistry;

public class ClientHandler implements Runnable, ClientSession {
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean isConnected;
    private UserProfile userProfile;
    private boolean admin;
    private int sessionId;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isConnected = true;
        try {
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            Log.error("Failed to get I/O streams", e);
            this.isConnected = false;
        }
        if (isConnected) {
            SessionRegistry.getInstance().register(this);
        } else {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
                // already failing; nothing more to do
            }
        }
    }

    @Override
    public void run() {
        while (isConnected) {
            try {
                Packet packet = PacketBuilder.readFromStream(in);
                PacketRouter.route(packet, this);
            } catch (IOException e) {
                Log.info("Client disconnected or error reading packet: " + e.getMessage());
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
            Log.error("Failed to send packet", e);
            disconnect();
        }
    }

    public void disconnect() {
        if (this.isConnected) {
            this.isConnected = false;
            SessionRegistry.getInstance().unregister(this);
            RoomManager.getInstance().leaveRoom(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getUsername() {
        return userProfile != null ? userProfile.getUsername() : null;
    }

    @Override
    public Integer getUserId() {
        return userProfile != null ? userProfile.getId() : null;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public void send(Packet packet) {
        sendPacket(packet);
    }

    @Override
    public void forceDisconnect() {
        disconnect();
    }
}
