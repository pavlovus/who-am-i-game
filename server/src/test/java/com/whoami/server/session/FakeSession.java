package com.whoami.server.session;

import com.whoami.protocol.packets.Packet;

import java.util.ArrayList;
import java.util.List;

/** Minimal in-memory {@link ClientSession} so admin/registry logic needs no sockets. */
public class FakeSession implements ClientSession {

    private int sessionId;
    private final String username;
    private final Integer userId;
    private final boolean admin;
    private boolean disconnected;
    private final List<Packet> sent = new ArrayList<>();

    public FakeSession(String username, Integer userId, boolean admin) {
        this.username = username;
        this.userId = userId;
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
        return username;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public void send(Packet packet) {
        sent.add(packet);
    }

    @Override
    public void forceDisconnect() {
        disconnected = true;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public List<Packet> getSent() {
        return sent;
    }
}
