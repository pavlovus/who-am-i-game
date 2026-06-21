package com.whoami.protocol.packets;

import com.whoami.protocol.crypto.Crc16;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketBuilder {

    public static byte[] toBytes(Packet packet) {
        int bufferSize = 13 + 2 + packet.getPayloadLength() + 2;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        buffer.put(packet.getMagicByte());
        buffer.putInt(packet.getClientId());
        buffer.putInt(packet.getPacketType());
        buffer.putInt(packet.getPayloadLength());

        byte[] header = new byte[13];
        buffer.position(0);
        buffer.get(header, 0, 13);
        short headerCrc = Crc16.calculateCrc(header);
        
        buffer.position(13);
        buffer.putShort(headerCrc);

        if (packet.getPayloadLength() > 0) {
            buffer.put(packet.getPayload());
            short payloadCrc = Crc16.calculateCrc(packet.getPayload());
            buffer.putShort(payloadCrc);
        } else {
            buffer.putShort((short) 0);
        }

        return buffer.array();
    }

    public static Packet readFromStream(DataInputStream in) throws IOException {
        byte magicByte = in.readByte();
        if (magicByte != Packet.MAGIC_BYTE) {
            throw new IOException("Invalid magic byte. Expected: " + Packet.MAGIC_BYTE + ", received: " + magicByte);
        }

        int clientId = in.readInt();
        int packetType = in.readInt();
        int payloadLength = in.readInt();
        
        ByteBuffer headerBuffer = ByteBuffer.allocate(13);
        headerBuffer.put(magicByte);
        headerBuffer.putInt(clientId);
        headerBuffer.putInt(packetType);
        headerBuffer.putInt(payloadLength);
        
        short expectedHeaderCrc = Crc16.calculateCrc(headerBuffer.array());
        short receivedHeaderCrc = in.readShort();

        if (expectedHeaderCrc != receivedHeaderCrc) {
            throw new IOException("Header CRC mismatch");
        }

        byte[] payload = new byte[payloadLength];
        if (payloadLength > 0) {
            in.readFully(payload);
        }

        short receivedPayloadCrc = in.readShort();
        short expectedPayloadCrc = payloadLength > 0 ? Crc16.calculateCrc(payload) : 0;

        if (expectedPayloadCrc != receivedPayloadCrc) {
            throw new IOException("Payload CRC mismatch");
        }

        return new Packet(magicByte, clientId, packetType, payloadLength, receivedHeaderCrc, payload);
    }
}
