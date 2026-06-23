package com.whoami.server.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryCharacterRepositoryTest {

    @Test
    public void addAndListPreservesInsertionOrder() {
        InMemoryCharacterRepository repo = new InMemoryCharacterRepository();
        repo.add("First", "cat", CharacterRecord.APPROVED);
        repo.add("Second", "cat", CharacterRecord.PENDING);

        assertEquals(2, repo.listAll().size());
        assertEquals("First", repo.listAll().get(0).name());
    }

    @Test
    public void rejectsBlankName() {
        InMemoryCharacterRepository repo = new InMemoryCharacterRepository();
        assertThrows(IllegalArgumentException.class, () -> repo.add("  ", "cat", CharacterRecord.APPROVED));
    }

    @Test
    public void setStatusUpdatesExistingAndIgnoresUnknown() {
        InMemoryCharacterRepository repo = new InMemoryCharacterRepository();
        CharacterRecord record = repo.add("Hero", "cat", CharacterRecord.PENDING);

        assertTrue(repo.setStatus(record.id(), CharacterRecord.APPROVED));
        assertTrue(repo.listAll().get(0).isApproved());
        assertFalse(repo.setStatus(404, CharacterRecord.APPROVED));
    }

    @Test
    public void deleteRemovesRecord() {
        InMemoryCharacterRepository repo = new InMemoryCharacterRepository();
        CharacterRecord record = repo.add("Hero", "cat", CharacterRecord.APPROVED);

        assertTrue(repo.delete(record.id()));
        assertFalse(repo.delete(record.id()));
        assertTrue(repo.listAll().isEmpty());
    }
}
