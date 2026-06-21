package com.whoami.protocol.packets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PacketTypeTest {
    @Test
    public void testValidId() {
        assertEquals(PacketType.AUTH_REQUEST, PacketType.fromId(1));
        assertEquals(PacketType.GAME_OVER, PacketType.fromId(10));
    }

    @Test
    public void testInvalidIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromId(999), "Should throw IllegalArgumentException for unknown ID");
    }
}