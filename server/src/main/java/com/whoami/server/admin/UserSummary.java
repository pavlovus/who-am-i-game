package com.whoami.server.admin;

public record UserSummary(int id, String username, int gamesPlayed, int gamesWon, boolean blocked) {
}
