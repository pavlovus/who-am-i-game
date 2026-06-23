package com.whoami.server.admin;

import java.util.List;

/**
 * Storage of the character bank. Implemented twice: a JDBC version for
 * production and an in-memory version that keeps tests free of a database.
 */
public interface CharacterRepository {

    CharacterRecord add(String name, String category, String status);

    List<CharacterRecord> listAll();

    boolean delete(int id);

    boolean setStatus(int id, String status);
}
