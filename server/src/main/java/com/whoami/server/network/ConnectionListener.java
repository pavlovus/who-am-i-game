package com.whoami.server.network;

import com.whoami.protocol.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionListener implements Runnable {
    private final int port;
    private final ExecutorService threadPool;
    private boolean isRunning;

    public ConnectionListener(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(100);
        this.isRunning = true;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Log.info("Server is listening for connections...");
            
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                Log.info("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.submit(clientHandler);
            }
        } catch (IOException e) {
            Log.error("Server exception", e);
        } finally {
            threadPool.shutdown();
        }
    }
    
    public void stop() {
        this.isRunning = false;
    }
}
