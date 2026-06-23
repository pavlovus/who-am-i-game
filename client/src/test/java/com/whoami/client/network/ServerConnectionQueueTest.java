package com.whoami.client.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.protocol.packets.PacketType;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConnectionQueueTest {

    @Test
    public void packetsSentWhileDisconnectedAreFlushedOnConnect() throws Exception {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        BlockingQueue<Integer> received = new LinkedBlockingQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);

        Thread serverThread = new Thread(() -> {
            try {
                Socket s = server.accept();
                DataInputStream in = new DataInputStream(s.getInputStream());
                while (running.get()) {
                    received.add(PacketBuilder.readFromStream(in).getPacketType());
                }
            } catch (Exception ignored) {
                // socket closed on shutdown
            }
        });
        serverThread.start();

        ServerConnection connection = new ServerConnection("127.0.0.1", port);
        // Sent BEFORE connect(): must be buffered, not dropped.
        connection.sendPacket(new Packet(Packet.MAGIC_BYTE, 0,
                PacketType.AUTH_REQUEST.getId(), 0, (short) 0, new byte[0]));

        connection.connect();

        Integer type = received.poll(2, TimeUnit.SECONDS);
        assertNotNull(type, "queued packet should be delivered after connect");
        assertEquals(PacketType.AUTH_REQUEST.getId(), type);

        connection.disconnect();
        running.set(false);
        server.close();
        serverThread.join(2000);
    }
}
