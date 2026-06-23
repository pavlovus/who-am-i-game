package com.whoami.server.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryGameResultRepository implements GameResultRepository {

    private final List<GameResult> results = new CopyOnWriteArrayList<>();

    @Override
    public void record(GameResult result) {
        results.add(result);
    }

    public List<GameResult> all() {
        return List.copyOf(results);
    }
}
