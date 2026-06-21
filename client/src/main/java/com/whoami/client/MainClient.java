package com.whoami.client;

import com.whoami.client.network.ServerConnection;

public class MainClient {
    public static void main(String[] args) {
        System.out.println("Starting WhoAmI Client...");
        
        ServerConnection connection = new ServerConnection("localhost", 8080);
        connection.connect();
        
        // TODO: Start UI
    }
}
