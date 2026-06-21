package com.whoami.protocol.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class Crc16Test {

    @Test
    public void testCalculateCrcConsistency() {
        byte[] data = "Hello, WhoAmI!".getBytes(StandardCharsets.UTF_8);
        short crc1 = Crc16.calculateCrc(data);
        short crc2 = Crc16.calculateCrc(data);
        
        assertEquals(crc1, crc2, "CRC should be consistent for the same input");
    }

    @Test
    public void testCalculateCrcDifference() {
        byte[] data1 = "Player1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "Player2".getBytes(StandardCharsets.UTF_8);
        
        short crc1 = Crc16.calculateCrc(data1);
        short crc2 = Crc16.calculateCrc(data2);
        
        assertNotEquals(crc1, crc2, "CRC should differ for different inputs");
    }

    @Test
    public void testEmptyArray() {
        assertEquals((short) 0, Crc16.calculateCrc(new byte[0]), "CRC of empty array should be 0");
    }
}