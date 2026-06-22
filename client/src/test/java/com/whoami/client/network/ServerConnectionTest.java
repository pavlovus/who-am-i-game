package com.whoami.client.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.protocol.packets.PacketType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConnectionTest {

    private static ServerSocket testServer;
    private static int port;
    private static Thread serverThread;
    
    // We will keep a reference to the accepted socket to verify data sent by the client
    private static Socket acceptedSocket;
    private static DataInputStream serverIn;
    private static DataOutputStream serverOut;

    @BeforeAll
    public static void setUpServer() throws IOException {
        testServer = new ServerSocket(0); // Bind to random available port
        port = testServer.getLocalPort();
        
        serverThread = new Thread(() -> {
            try {
                acceptedSocket = testServer.accept();
                serverIn = new DataInputStream(acceptedSocket.getInputStream());
                serverOut = new DataOutputStream(acceptedSocket.getOutputStream());
            } catch (IOException e) {
                // Ignore, normal during shutdown
            }
        });
        serverThread.start();
    }

    @AfterAll
    public static void tearDownServer() throws IOException, InterruptedException {
        if (testServer != null && !testServer.isClosed()) {
            testServer.close();
        }
        if (acceptedSocket != null && !acceptedSocket.isClosed()) {
            acceptedSocket.close();
        }
        if (serverThread != null) {
            serverThread.join(2000);
        }
    }

    @Test
    public void testConnectAndSendPacket() throws Exception {
        ServerConnection connection = new ServerConnection("127.0.0.1", port);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean disconnectedFired = new AtomicBoolean(false);
        
        connection.setPacketListener(new PacketListener() {
            @Override
            public void onPacketReceived(Packet packet) {
                if (packet.getPacketType() == PacketType.ROOM_CREATE.getId()) {
                    latch.countDown();
                }
            }

            @Override
            public void onDisconnected() {
                disconnectedFired.set(true);
            }
        });

        // 1. Connect
        connection.connect();
        
        // Wait a tiny bit for the server thread to accept
        Thread.sleep(100);
        assertNotNull(serverIn, "Server should have accepted the connection");

        // 2. Send Packet from Client -> Server
        Packet authPacket = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), 0, (short)0, new byte[0]);
        connection.sendPacket(authPacket);
        
        // Server verifies it received the packet
        Packet receivedByServer = PacketBuilder.readFromStream(serverIn);
        assertNotNull(receivedByServer);
        assertEquals(PacketType.AUTH_REQUEST.getId(), receivedByServer.getPacketType());
        
        // 3. Send Packet from Server -> Client
        Packet roomPacket = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_CREATE.getId(), 0, (short)0, new byte[0]);
        serverOut.write(PacketBuilder.toBytes(roomPacket));
        serverOut.flush();
        
        // Client listener should receive it
        boolean receivedByClient = latch.await(2, TimeUnit.SECONDS);
        assertTrue(receivedByClient, "Client should have received the ROOM_CREATE packet from the server");
        
        // 4. Disconnect safely
        connection.disconnect();
        
        // The listener onDisconnected might fire asynchronously when the reading thread crashes.
        // Or manually called. We don't strictly test that it fired instantly, just that disconnect() didn't crash
        assertDoesNotThrow(connection::disconnect, "Disconnect should be idempotent and not throw");
    }

    @Test
    public void testFailedConnection() {
        // Try connecting to a port we know is not listening (e.g. random unused port)
        ServerConnection connection = new ServerConnection("127.0.0.1", 1);
        assertDoesNotThrow(connection::connect, "Connecting to a dead port should not throw exceptions but handle them gracefully");
        
        // sendPacket should fail silently if not connected
        Packet dummy = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), 0, (short)0, new byte[0]);
        assertDoesNotThrow(() -> connection.sendPacket(dummy), "Sending packet when disconnected should be handled gracefully");
    }
}
