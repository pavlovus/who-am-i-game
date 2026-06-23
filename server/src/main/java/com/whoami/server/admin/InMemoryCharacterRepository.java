package com.whoami.server.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryCharacterRepository implements CharacterRepository {

    private final Map<Integer, CharacterRecord> characters = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    @Override
    public CharacterRecord add(String name, String category, String status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Character name is required");
        }
        int id = idSequence.incrementAndGet();
        CharacterRecord record = new CharacterRecord(id, name.trim(), category, status);
        characters.put(id, record);
        return record;
    }

    @Override
    public List<CharacterRecord> listAll() {
        List<CharacterRecord> all = new ArrayList<>(characters.values());
        all.sort((a, b) -> Integer.compare(a.id(), b.id()));
        return all;
    }

    @Override
    public boolean delete(int id) {
        return characters.remove(id) != null;
    }

    @Override
    public boolean setStatus(int id, String status) {
        CharacterRecord existing = characters.get(id);
        if (existing == null) {
            return false;
        }
        characters.put(id, new CharacterRecord(id, existing.name(), existing.category(), status));
        return true;
    }
}
