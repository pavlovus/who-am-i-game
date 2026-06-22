package com.whoami.protocol.packets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PacketTypeTest {
    @Test
    public void testValidId() {
        assertEquals(PacketType.AUTH_REQUEST, PacketType.fromId(1));
        assertEquals(PacketType.GAME_OVER, PacketType.fromId(10));
        assertEquals(PacketType.ROOM_LEAVE, PacketType.fromId(12));
    }

    @Test
    public void testInvalidIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromId(999), "Should throw IllegalArgumentException for unknown ID");
    }

    @Test
    public void testAllValuesHaveUniqueIds() {
        long uniqueCount = java.util.Arrays.stream(PacketType.values())
                .map(PacketType::getId)
                .distinct()
                .count();
        assertEquals(PacketType.values().length, uniqueCount, "All PacketTypes should have unique IDs");
    }
}