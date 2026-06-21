package com.whoami.protocol.packets;

public class Packet {
    private final byte magicByte;
    private final int clientId;
    private final int packetType;
    private final int payloadLength;
    private final short crc16;
    private final byte[] payload;

    public static final byte MAGIC_BYTE = 0x13;

    public Packet(byte magicByte, int clientId, int packetType, int payloadLength, short crc16, byte[] payload) {
        this.magicByte = magicByte;
        this.clientId = clientId;
        this.packetType = packetType;
        this.payloadLength = payloadLength;
        this.crc16 = crc16;
        this.payload = payload;
    }

    public byte getMagicByte() {
        return magicByte;
    }

    public int getClientId() {
        return clientId;
    }

    public int getPacketType() {
        return packetType;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public short getCrc16() {
        return crc16;
    }

    public byte[] getPayload() {
        return payload;
    }
}
