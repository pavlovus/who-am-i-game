package com.whoami.protocol.packets;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PacketBuilderTest {

    @Test
    public void testToBytesAndReadFromStream() throws IOException {
        int clientId = 1001;
        int packetType = PacketType.GAME_START.getId();
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        
        // Serialize
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
        DataInputStream dis = new DataInputStream(bais);
        
        Packet reconstructedPacket = PacketBuilder.readFromStream(dis);
        
        assertEquals(Packet.MAGIC_BYTE, reconstructedPacket.getMagicByte());
        assertEquals(clientId, reconstructedPacket.getClientId());
        assertEquals(packetType, reconstructedPacket.getPacketType());
        assertEquals(payload.length, reconstructedPacket.getPayloadLength());
        assertArrayEquals(payload, reconstructedPacket.getPayload());
    }

    @Test
    public void testInvalidMagicByteThrowsException() {
        int clientId = 1001;
        int packetType = PacketType.AUTH_REQUEST.getId();
        byte[] payload = new byte[]{};
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        // Tamper magic byte
        rawBytes[0] = 0x00;
        
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
        DataInputStream dis = new DataInputStream(bais);
        
        assertThrows(IOException.class, () -> PacketBuilder.readFromStream(dis), "Should throw IOException on invalid magic byte");
    }

    @Test
    public void testInvalidHeaderCrcThrowsException() {
        int clientId = 1001;
        int packetType = PacketType.AUTH_REQUEST.getId();
        byte[] payload = new byte[]{};
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        // Tamper client id inside header (bytes 1-4) to invalidate CRC
        rawBytes[1] = (byte) ~rawBytes[1];
        
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
        DataInputStream dis = new DataInputStream(bais);
        
        IOException exception = assertThrows(IOException.class, () -> PacketBuilder.readFromStream(dis));
        assertTrue(exception.getMessage().contains("Header CRC mismatch"));
    }

    @Test
    public void testInvalidPayloadCrcThrowsException() {
        int clientId = 1001;
        int packetType = PacketType.GAME_START.getId();
        byte[] payload = new byte[]{10, 20, 30};
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        // Tamper payload byte (starts after 15 bytes of header+crc)
        rawBytes[15] = (byte) ~rawBytes[15];
        
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
        DataInputStream dis = new DataInputStream(bais);
        
        IOException exception = assertThrows(IOException.class, () -> PacketBuilder.readFromStream(dis));
        assertTrue(exception.getMessage().contains("Payload CRC mismatch"));
    }

    @Test
    public void testEmptyPayloadPacket() throws IOException {
        int clientId = 1002;
        int packetType = PacketType.ROOM_CREATE.getId();
        byte[] payload = new byte[0];
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rawBytes));
        Packet reconstructedPacket = PacketBuilder.readFromStream(dis);
        
        assertEquals(0, reconstructedPacket.getPayloadLength());
        assertArrayEquals(payload, reconstructedPacket.getPayload());
    }

    @Test
    public void testIncompleteStreamThrowsEOF() {
        int clientId = 1001;
        int packetType = PacketType.GAME_START.getId();
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        
        Packet originalPacket = new Packet(Packet.MAGIC_BYTE, clientId, packetType, payload.length, (short) 0, payload);
        byte[] rawBytes = PacketBuilder.toBytes(originalPacket);
        
        // Truncate the byte array
        byte[] truncatedBytes = new byte[10];
        System.arraycopy(rawBytes, 0, truncatedBytes, 0, 10);
        
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(truncatedBytes));
        
        assertThrows(java.io.EOFException.class, () -> PacketBuilder.readFromStream(dis), "Should throw EOFException when stream ends unexpectedly");
    }
}