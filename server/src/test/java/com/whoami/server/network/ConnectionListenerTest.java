package com.whoami.server.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ConnectionListenerTest {

    @Test
    public void testListenerLifecycle() throws InterruptedException {
        int testPort = 8200;
        ConnectionListener listener = new ConnectionListener(testPort);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
        
        // Give it a moment to start
        Thread.sleep(200);
        
        // Stop it
        listener.stop();
        
        // The listener is blocked on serverSocket.accept(), we need to connect to unblock it and let it exit naturally
        assertDoesNotThrow(() -> {
            new Socket("127.0.0.1", testPort).close();
        });
        
        // Wait for it to die
        listenerThread.join(2000);
        
        // Assert it successfully died and didn't hang forever
        org.junit.jupiter.api.Assertions.assertFalse(listenerThread.isAlive(), "ConnectionListener thread should terminate when stopped and interrupted");
    }
}
