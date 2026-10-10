package com.xujiache.jingweitoolbox.auth;

public record UserAccount(long id, String identifier, String passwordHash, String displayName, UserRole role,
        UserStatus status, long version) {
    public String idAsString() {
        return Long.toString(id);
    }

    public boolean active() {
        return status == UserStatus.ACTIVE;
    }
}
