package com.whoami.server.session;

import com.whoami.protocol.packets.Packet;

/**
 * Abstraction over a connected client. {@code ClientHandler} is the real
 * implementation; tests use lightweight fakes so the admin/registry logic can
 * be verified without opening sockets.
 */
public interface ClientSession {

    int getSessionId();

    void setSessionId(int sessionId);

    String getUsername();

    Integer getUserId();

    boolean isAdmin();

    void send(Packet packet);

    void forceDisconnect();
}
