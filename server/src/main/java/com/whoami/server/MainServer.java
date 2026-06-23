package com.whoami.server;

import com.whoami.protocol.util.Log;
import com.whoami.server.admin.AdminConsole;
import com.whoami.server.admin.AdminServices;
import com.whoami.server.network.ConnectionListener;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class MainServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        Log.setEnabled(true);
        Log.info("Starting WhoAmI Server on port " + PORT + "...");

        try {
            com.whoami.server.database.ConnectionPool.init();
        } catch (Exception e) {
            Log.error("Failed to initialize database", e);
            System.exit(1);
        }

        ConnectionListener listener = new ConnectionListener(PORT);
        new Thread(listener).start();

        boolean noGui = Arrays.asList(args).contains("--no-gui");
        if (!noGui && !GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> new AdminConsole(AdminServices.get()).setVisible(true));
        }
    }
}
