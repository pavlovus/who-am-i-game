package com.whoami.client.state;

import com.whoami.client.network.ServerConnection;

public class ClientContext {
    private static ClientContext instance;

    private String jwtToken;
    private String username;
    private ServerConnection serverConnection;

    private ClientContext() {
        // We initialize the connection to localhost by default
        serverConnection = new ServerConnection("localhost", 8080);
        serverConnection.connect();
    }

    public static synchronized ClientContext getInstance() {
        if (instance == null) {
            instance = new ClientContext();
        }
        return instance;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void disconnect() {
        if (serverConnection != null) {
            serverConnection.disconnect();
        }
    }
}
