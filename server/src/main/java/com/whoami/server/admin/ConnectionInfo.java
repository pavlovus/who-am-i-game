package com.whoami.server.admin;

public record ConnectionInfo(int sessionId, String username, boolean authenticated, boolean admin) {
}
