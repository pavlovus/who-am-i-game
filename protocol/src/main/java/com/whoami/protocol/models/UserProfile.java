package com.whoami.protocol.models;

public class UserProfile {
    private int id;
    private String username;
    private int gamesPlayed;
    private int gamesWon;

    public UserProfile(int id, String username, int gamesPlayed, int gamesWon) {
        this.id = id;
        this.username = username;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", gamesPlayed=" + gamesPlayed +
                ", gamesWon=" + gamesWon +
                '}';
    }
}
