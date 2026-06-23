package com.whoami.server.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryUserAdminRepository implements UserAdminRepository {

    private final Map<Integer, UserSummary> users = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    public UserSummary addUser(String username, int gamesPlayed, int gamesWon, boolean blocked) {
        int id = idSequence.incrementAndGet();
        UserSummary summary = new UserSummary(id, username, gamesPlayed, gamesWon, blocked);
        users.put(id, summary);
        return summary;
    }

    @Override
    public List<UserSummary> listAll() {
        List<UserSummary> all = new ArrayList<>(users.values());
        all.sort((a, b) -> Integer.compare(a.id(), b.id()));
        return all;
    }

    @Override
    public boolean setBlocked(int userId, boolean blocked) {
        UserSummary existing = users.get(userId);
        if (existing == null) {
            return false;
        }
        users.put(userId, new UserSummary(existing.id(), existing.username(),
                existing.gamesPlayed(), existing.gamesWon(), blocked));
        return true;
    }

    @Override
    public boolean isBlocked(String username) {
        return users.values().stream()
                .filter(u -> u.username().equals(username))
                .anyMatch(UserSummary::blocked);
    }
}
