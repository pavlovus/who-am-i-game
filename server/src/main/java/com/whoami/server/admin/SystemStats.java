package com.whoami.server.admin;

public record SystemStats(int activeConnections,
                          int totalUsers,
                          int blockedUsers,
                          int totalCharacters,
                          int approvedCharacters) {
}
