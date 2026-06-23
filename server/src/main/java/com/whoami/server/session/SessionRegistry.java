package com.whoami.server.session;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe registry of every live connection. Backed by a
 * {@link ConcurrentHashMap} keyed by an {@link AtomicInteger}-generated id so
 * the admin panel can list connections and forcibly kick them.
 */
public class SessionRegistry {

    private static final SessionRegistry INSTANCE = new SessionRegistry();

    private final ConcurrentHashMap<Integer, ClientSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    public static SessionRegistry getInstance() {
        return INSTANCE;
    }

    public int register(ClientSession session) {
        int id = idSequence.incrementAndGet();
        session.setSessionId(id);
        sessions.put(id, session);
        return id;
    }

    public void unregister(ClientSession session) {
        if (session != null) {
            sessions.remove(session.getSessionId());
        }
    }

    public ClientSession get(int sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<ClientSession> list() {
        return List.copyOf(sessions.values());
    }

    public int count() {
        return sessions.size();
    }

    /** Forcibly drops a connection by id. Returns true if a session was kicked. */
    public boolean kick(int sessionId) {
        ClientSession session = sessions.remove(sessionId);
        if (session == null) {
            return false;
        }
        session.forceDisconnect();
        return true;
    }
}
