package com.whoami.server.admin;

import com.whoami.server.session.SessionRegistry;

/**
 * Holds the process-wide {@link AdminService}. Defaults to JDBC-backed
 * repositories but can be replaced (e.g. by tests or an in-memory demo run).
 */
public final class AdminServices {

    private static AdminService instance;

    private AdminServices() {
    }

    public static synchronized AdminService get() {
        if (instance == null) {
            instance = new AdminService(
                    SessionRegistry.getInstance(),
                    new JdbcCharacterRepository(),
                    new JdbcUserAdminRepository());
        }
        return instance;
    }

    public static synchronized void set(AdminService service) {
        instance = service;
    }
}
