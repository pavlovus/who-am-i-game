package com.whoami.server.admin;

import com.whoami.server.session.ClientSession;
import com.whoami.server.session.SessionRegistry;

import java.util.List;

/**
 * Business logic behind the admin panel. Depends only on small interfaces so it
 * is fully unit-testable with in-memory fakes (no socket, no database).
 */
public class AdminService {

    private final SessionRegistry registry;
    private final CharacterRepository characters;
    private final UserAdminRepository users;

    public AdminService(SessionRegistry registry,
                        CharacterRepository characters,
                        UserAdminRepository users) {
        this.registry = registry;
        this.characters = characters;
        this.users = users;
    }

    public List<ConnectionInfo> listConnections() {
        return registry.list().stream()
                .map(s -> new ConnectionInfo(s.getSessionId(), s.getUsername(),
                        s.getUserId() != null, s.isAdmin()))
                .sorted((a, b) -> Integer.compare(a.sessionId(), b.sessionId()))
                .toList();
    }

    public boolean kickConnection(int sessionId) {
        return registry.kick(sessionId);
    }

    /** Marks a user as blocked and force-disconnects any of their live sessions. */
    public boolean blockUser(int userId) {
        boolean updated = users.setBlocked(userId, true);
        if (updated) {
            kickSessionsOf(userId);
        }
        return updated;
    }

    public boolean unblockUser(int userId) {
        return users.setBlocked(userId, false);
    }

    private void kickSessionsOf(int userId) {
        for (ClientSession session : registry.list()) {
            Integer id = session.getUserId();
            if (id != null && id == userId) {
                registry.kick(session.getSessionId());
            }
        }
    }

    public CharacterRecord addCharacter(String name, String category) {
        return characters.add(name, category, CharacterRecord.APPROVED);
    }

    public CharacterRecord proposeCharacter(String name, String category) {
        return characters.add(name, category, CharacterRecord.PENDING);
    }

    public List<CharacterRecord> listCharacters() {
        return characters.listAll();
    }

    public boolean deleteCharacter(int id) {
        return characters.delete(id);
    }

    public boolean approveCharacter(int id) {
        return characters.setStatus(id, CharacterRecord.APPROVED);
    }

    public List<UserSummary> listUsers() {
        return users.listAll();
    }

    public SystemStats systemStats() {
        List<UserSummary> allUsers = users.listAll();
        List<CharacterRecord> allCharacters = characters.listAll();
        int blocked = (int) allUsers.stream().filter(UserSummary::blocked).count();
        int approved = (int) allCharacters.stream().filter(CharacterRecord::isApproved).count();
        return new SystemStats(registry.count(), allUsers.size(), blocked,
                allCharacters.size(), approved);
    }
}
