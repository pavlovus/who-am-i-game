package com.whoami.server.network;

import com.whoami.server.game.GameLogic;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    public enum State { WAITING, SELECTING, IN_PROGRESS, FINISHED }

    private final String roomCode;
    private ClientHandler player1; // Host
    private ClientHandler player2; // Guest
    private String characterName;
    private ClientHandler riddler;
    private ClientHandler guesser;
    private volatile State state = State.WAITING;
    private GameLogic logic;

    private final Set<ClientHandler> rematchVotes = ConcurrentHashMap.newKeySet();

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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public GameLogic getLogic() {
        return logic;
    }

    public void setLogic(GameLogic logic) {
        this.logic = logic;
    }

    public boolean contains(ClientHandler client) {
        return client != null && (client == player1 || client == player2);
    }

    public ClientHandler getOpponent(ClientHandler client) {
        if (client == player1) {
            return player2;
        }
        if (client == player2) {
            return player1;
        }
        return null;
    }

    /**
     * Records a rematch vote. Returns true only once both players in a full room
     * have agreed, at which point the caller should reset and restart the round.
     */
    public synchronized boolean requestRematch(ClientHandler client) {
        if (!contains(client)) {
            return false;
        }
        rematchVotes.add(client);
        return isFull()
                && rematchVotes.contains(player1)
                && rematchVotes.contains(player2);
    }

    public synchronized void resetForRematch() {
        characterName = null;
        riddler = null;
        guesser = null;
        logic = null;
        rematchVotes.clear();
        state = State.WAITING;
    }
}
