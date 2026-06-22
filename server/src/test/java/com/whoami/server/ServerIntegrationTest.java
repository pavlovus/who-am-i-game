package com.whoami.server;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.network.ConnectionListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ServerIntegrationTest {

    private static ConnectionListener connectionListener;
    private static Thread serverThread;
    private static final int TEST_PORT = 8199;

    @BeforeAll
    public static void startServer() throws InterruptedException {
        connectionListener = new ConnectionListener(TEST_PORT);
        serverThread = new Thread(connectionListener);
        serverThread.start();
        
        // Wait briefly for server socket to bind
        Thread.sleep(500);
    }

    @AfterAll
    public static void stopServer() {
        if (connectionListener != null) {
            connectionListener.stop();
        }
        try {
            // Unblock accept() by connecting once
            new Socket("127.0.0.1", TEST_PORT).close();
            serverThread.join(2000);
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void testFullRoomCreateFlow() throws IOException {
        try (Socket clientSocket = new Socket("127.0.0.1", TEST_PORT);
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream in = new DataInputStream(clientSocket.getInputStream())) {

            // 1. Send ROOM_CREATE packet
            Packet requestPacket = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_CREATE.getId(), 0, (short)0, new byte[0]);
            byte[] rawBytes = PacketBuilder.toBytes(requestPacket);
            out.write(rawBytes);
            out.flush();

            // 2. Read response
            Packet responsePacket = PacketBuilder.readFromStream(in);
            
            // 3. Verify
            assertNotNull(responsePacket, "Should receive a response packet");
            assertEquals(PacketType.ROOM_CREATE.getId(), responsePacket.getPacketType(), "Should respond with ROOM_CREATE type");
            
            String payloadStr = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);
            assertTrue(payloadStr.startsWith("SUCCESS:"), "Response should be SUCCESS:CODE");
            assertEquals(14, payloadStr.length(), "Payload 'SUCCESS:XXXXXX' should be 14 chars long");
        }
    }
}
