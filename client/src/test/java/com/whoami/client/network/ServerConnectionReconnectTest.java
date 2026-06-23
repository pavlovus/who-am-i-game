package com.whoami.client.network;

import com.whoami.protocol.net.ReconnectPolicy;
import com.whoami.protocol.packets.Packet;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConnectionReconnectTest {

    @Test
    public void reconnectsAfterUnexpectedDrop() throws Exception {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        CopyOnWriteArrayList<Socket> accepted = new CopyOnWriteArrayList<>();
        CountDownLatch firstAccept = new CountDownLatch(1);
        CountDownLatch secondAccept = new CountDownLatch(2);
        AtomicBoolean running = new AtomicBoolean(true);

        Thread serverThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket s = server.accept();
                    accepted.add(s);
                    firstAccept.countDown();
                    secondAccept.countDown();
                } catch (Exception e) {
                    return;
                }
            }
        });
        serverThread.start();

        CountDownLatch reconnected = new CountDownLatch(1);
        ServerConnection connection = new ServerConnection("127.0.0.1", port,
                new ReconnectPolicy(5, 50, 200));
        connection.setPacketListener(new PacketListener() {
            @Override public void onPacketReceived(Packet packet) { }
            @Override public void onDisconnected() { }
            @Override public void onReconnected() { reconnected.countDown(); }
        });

        connection.connect();
        assertTrue(firstAccept.await(2, TimeUnit.SECONDS), "server should accept the first connection");

        // Simulate an unexpected drop by closing the server side of the socket.
        accepted.get(0).close();

        assertTrue(reconnected.await(3, TimeUnit.SECONDS), "client should transparently reconnect");
        assertTrue(secondAccept.await(1, TimeUnit.SECONDS), "server should have accepted a second connection");
        assertTrue(connection.isConnected());

        connection.disconnect();
        running.set(false);
        server.close();
        serverThread.join(2000);
    }
}
