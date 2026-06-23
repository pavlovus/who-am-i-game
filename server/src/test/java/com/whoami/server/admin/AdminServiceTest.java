package com.whoami.server.admin;

import com.whoami.server.session.FakeSession;
import com.whoami.server.session.SessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest {

    private SessionRegistry registry;
    private InMemoryCharacterRepository characters;
    private InMemoryUserAdminRepository users;
    private AdminService admin;

    @BeforeEach
    public void setUp() {
        registry = new SessionRegistry();
        characters = new InMemoryCharacterRepository();
        users = new InMemoryUserAdminRepository();
        admin = new AdminService(registry, characters, users);
    }

    @Test
    public void listsConnectionsSortedById() {
        registry.register(new FakeSession("bob", 2, false));
        registry.register(new FakeSession("admin", 1, true));

        List<ConnectionInfo> connections = admin.listConnections();

        assertEquals(2, connections.size());
        assertTrue(connections.get(0).sessionId() < connections.get(1).sessionId());
    }

    @Test
    public void kickConnectionDropsSession() {
        FakeSession session = new FakeSession("bob", 2, false);
        int id = registry.register(session);

        assertTrue(admin.kickConnection(id));
        assertTrue(session.isDisconnected());
    }

    @Test
    public void blockUserMarksBlockedAndKicksLiveSessions() {
        UserSummary user = users.addUser("cheater", 3, 1, false);
        FakeSession session = new FakeSession("cheater", user.id(), false);
        registry.register(session);

        assertTrue(admin.blockUser(user.id()));

        assertTrue(users.isBlocked("cheater"));
        assertTrue(session.isDisconnected(), "active sessions of a blocked user must be kicked");
    }

    @Test
    public void blockUnknownUserReturnsFalse() {
        assertFalse(admin.blockUser(999));
    }

    @Test
    public void characterBankAddApproveDelete() {
        CharacterRecord pending = admin.proposeCharacter("Mario", "Video Games");
        assertEquals(CharacterRecord.PENDING, pending.status());

        assertTrue(admin.approveCharacter(pending.id()));
        assertTrue(admin.listCharacters().get(0).isApproved());

        assertTrue(admin.deleteCharacter(pending.id()));
        assertTrue(admin.listCharacters().isEmpty());
    }

    @Test
    public void systemStatsAggregatesEverything() {
        users.addUser("a", 0, 0, false);
        users.addUser("b", 0, 0, true);
        admin.addCharacter("Approved", "cat");
        admin.proposeCharacter("Pending", "cat");
        registry.register(new FakeSession("a", 1, false));

        SystemStats stats = admin.systemStats();

        assertEquals(1, stats.activeConnections());
        assertEquals(2, stats.totalUsers());
        assertEquals(1, stats.blockedUsers());
        assertEquals(2, stats.totalCharacters());
        assertEquals(1, stats.approvedCharacters());
    }
}
