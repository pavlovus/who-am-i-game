package com.whoami.server.game;

/** Process-wide {@link GameResultRepository}; JDBC by default, swappable in tests. */
public final class GameResults {

    private static GameResultRepository instance;

    private GameResults() {
    }

    public static synchronized GameResultRepository get() {
        if (instance == null) {
            instance = new JdbcGameResultRepository();
        }
        return instance;
    }

    public static synchronized void set(GameResultRepository repository) {
        instance = repository;
    }
}
