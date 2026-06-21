package com.whoami.server;

import com.whoami.server.network.ConnectionListener;

public class MainServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("Starting WhoAmI Server on port " + PORT + "...");
        
        // Init Database
        try {
            com.whoami.server.database.ConnectionPool.init();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        ConnectionListener listener = new ConnectionListener(PORT);
        new Thread(listener).start();
    }
}
