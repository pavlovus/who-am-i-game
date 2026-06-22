package com.whoami.server.network;

public class GameRoom {
    private final String roomCode;
    private ClientHandler player1; // Host (Riddler usually, but we'll randomize)
    private ClientHandler player2; // Guest
    private String characterName;
    private ClientHandler riddler;
    private ClientHandler guesser;

    public GameRoom(String roomCode, ClientHandler host) {
        this.roomCode = roomCode;
        this.player1 = host;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public ClientHandler getPlayer1() {
        return player1;
    }

    public ClientHandler getPlayer2() {
        return player2;
    }

    public void setPlayer2(ClientHandler player2) {
        this.player2 = player2;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public ClientHandler getRiddler() {
        return riddler;
    }

    public void setRiddler(ClientHandler riddler) {
        this.riddler = riddler;
    }

    public ClientHandler getGuesser() {
        return guesser;
    }

    public void setGuesser(ClientHandler guesser) {
        this.guesser = guesser;
    }

    public boolean isFull() {
        return player1 != null && player2 != null;
    }
}
