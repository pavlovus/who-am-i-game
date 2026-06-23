package com.whoami.server.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionRegistryTest {

    @Test
    public void registerAssignsUniqueIds() {
        SessionRegistry registry = new SessionRegistry();
        FakeSession a = new FakeSession("a", 1, false);
        FakeSession b = new FakeSession("b", 2, false);

        int idA = registry.register(a);
        int idB = registry.register(b);

        assertNotEquals(idA, idB);
        assertEquals(idA, a.getSessionId());
        assertEquals(2, registry.count());
        assertSame(a, registry.get(idA));
    }

    @Test
    public void unregisterRemovesSession() {
        SessionRegistry registry = new SessionRegistry();
        FakeSession a = new FakeSession("a", 1, false);
        registry.register(a);

        registry.unregister(a);

        assertEquals(0, registry.count());
        assertNull(registry.get(a.getSessionId()));
    }

    @Test
    public void kickDisconnectsAndRemoves() {
        SessionRegistry registry = new SessionRegistry();
        FakeSession a = new FakeSession("a", 1, false);
        int id = registry.register(a);

        assertTrue(registry.kick(id));
        assertTrue(a.isDisconnected());
        assertEquals(0, registry.count());
        assertFalse(registry.kick(id), "kicking an unknown session returns false");
    }
}
