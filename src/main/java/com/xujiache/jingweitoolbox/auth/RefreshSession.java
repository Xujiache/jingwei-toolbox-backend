package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.security.PrincipalType;
import java.time.Instant;

public record RefreshSession(long id, String sessionId, PrincipalType principalType, Long userId, String guestId,
        Instant expiresAt, Instant revokedAt) {
    public boolean active() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
