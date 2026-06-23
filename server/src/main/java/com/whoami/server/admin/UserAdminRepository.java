package com.whoami.server.admin;

import java.util.List;

public interface UserAdminRepository {

    List<UserSummary> listAll();

    boolean setBlocked(int userId, boolean blocked);

    boolean isBlocked(String username);
}
