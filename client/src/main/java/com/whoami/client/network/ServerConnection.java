package com.whoami.client.network;

import com.whoami.protocol.net.ReconnectPolicy;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketBuilder;
import com.whoami.protocol.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerConnection {
    private static final int MAX_PENDING = 100;

    private final String host;
    private final int port;
    private final ReconnectPolicy reconnectPolicy;
    private final Object writeLock = new Object();
    private final Queue<Packet> pending = new ConcurrentLinkedQueue<>();

    private volatile Socket socket;
    private volatile DataInputStream in;
    private volatile DataOutputStream out;
    private volatile boolean connected;
    private volatile boolean manualClose;
    private volatile boolean loopRunning;
    private PacketListener listener;

    public ServerConnection(String host, int port) {
        this(host, port, ReconnectPolicy.defaultPolicy());
    }

    public ServerConnection(String host, int port, ReconnectPolicy reconnectPolicy) {
        this.host = host;
        this.port = port;
        this.reconnectPolicy = reconnectPolicy;
    }

    public void setPacketListener(PacketListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    /** Opens the first connection. A failure here is reported, not retried. */
    public synchronized void connect() {
        if (connected || loopRunning) {
            return;
        }
        manualClose = false;
        try {
            openSocket();
            connected = true;
            loopRunning = true;
            reconnectPolicy.reset();
            Log.info("Connected to server at " + host + ":" + port);
            flushPending();
            new Thread(this::runConnectionLoop, "server-connection").start();
        } catch (IOException e) {
            Log.error("Connection failed", e);
            connected = false;
        }
    }

    private void openSocket() throws IOException {
        closeQuietly(socket);
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    private static void closeQuietly(Socket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private void runConnectionLoop() {
        try {
            while (!manualClose) {
                readUntilDrop();
                if (manualClose || !reconnect()) {
                    break;
                }
            }
            connected = false;
            if (listener != null) {
                listener.onDisconnected();
            }
        } finally {
            loopRunning = false;
        }
    }

    private void readUntilDrop() {
        while (connected && !manualClose) {
            try {
                Packet packet = PacketBuilder.readFromStream(in);
                if (listener != null) {
                    listener.onPacketReceived(packet);
                }
            } catch (IOException e) {
                connected = false;
                Log.info("Disconnected from server: " + e.getMessage());
            }
        }
    }

    /** Exponential-backoff reconnect. Returns true once the link is restored. */
    private boolean reconnect() {
        while (!manualClose && reconnectPolicy.canRetry()) {
            long delay = reconnectPolicy.nextDelayMs();
            Log.info("Reconnecting in " + delay + "ms (attempt " + reconnectPolicy.getAttempts() + ")");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (manualClose) {
                return false;
            }
            try {
                openSocket();
                connected = true;
                reconnectPolicy.reset();
                Log.info("Reconnected to server");
                flushPending();
                if (listener != null) {
                    listener.onReconnected();
                }
                return true;
            } catch (IOException e) {
                Log.info("Reconnect attempt failed: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Queues a packet and flushes it. Packets submitted while reconnecting are
     * buffered (bounded) and sent once the link is back, instead of being dropped.
     */
    public void sendPacket(Packet packet) {
        if (manualClose) {
            return;
        }
        if (pending.size() < MAX_PENDING) {
            pending.offer(packet);
        }
        if (connected) {
            flushPending();
        }
    }

    private void flushPending() {
        synchronized (writeLock) {
            Packet packet;
            while (connected && (packet = pending.peek()) != null) {
                try {
                    out.write(PacketBuilder.toBytes(packet));
                    out.flush();
                    pending.poll();
                } catch (IOException e) {
                    Log.error("Failed to send packet", e);
                    connected = false; // keep packet queued for the next reconnect
                    return;
                }
            }
        }
    }

    public void disconnect() {
        manualClose = true;
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
