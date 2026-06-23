package com.whoami.server.admin;

public record CharacterRecord(int id, String name, String category, String status) {

    public static final String APPROVED = "approved";
    public static final String PENDING = "pending";

    public boolean isApproved() {
        return APPROVED.equals(status);
    }
}
