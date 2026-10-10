package com.xujiache.jingweitoolbox.common.security;

import java.util.Set;

public record AuthenticatedPrincipal(String subject, PrincipalType principalType, String displayName, Set<String> scopes,
        String tokenId, String sessionId, long expiresAtEpochSeconds) {
    public boolean isGuest() {
        return principalType == PrincipalType.GUEST;
    }

    public boolean isAdmin() {
        return principalType == PrincipalType.ADMIN;
    }
}
