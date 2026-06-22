package com.whoami.client.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClientContextTest {

    @Test
    public void testSingleton() {
        ClientContext instance1 = ClientContext.getInstance();
        ClientContext instance2 = ClientContext.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "ClientContext should be a Singleton");
    }

    @Test
    public void testStateManagement() {
        ClientContext context = ClientContext.getInstance();

        // Test Setters and Getters
        context.setUsername("TestUser");
        assertEquals("TestUser", context.getUsername());

        context.setJwtToken("TestToken.123");
        assertEquals("TestToken.123", context.getJwtToken());

        context.setCurrentRoomCode("ABCDEF");
        assertEquals("ABCDEF", context.getCurrentRoomCode());

        context.setRole("Riddler");
        assertEquals("Riddler", context.getRole());

        context.setEncryptedCharacterName("HEXCODE");
        assertEquals("HEXCODE", context.getEncryptedCharacterName());
    }

    @Test
    public void testDisconnectSafely() {
        ClientContext context = ClientContext.getInstance();
        assertDoesNotThrow(context::disconnect, "Disconnect should execute safely even without an active server");
    }
}
