package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.security.PrincipalType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshSessionRepository {
    private final JdbcTemplate jdbc;

    public RefreshSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String sessionId, String tokenHash, PrincipalType principalType, Long userId, String guestId,
            Instant expiresAt) {
        jdbc.update("""
                INSERT INTO refresh_sessions(session_id, token_hash, principal_type, user_id, guest_id, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))
                """, sessionId, tokenHash, principalType.name(), userId, guestId, Timestamp.from(expiresAt));
    }

    public Optional<RefreshSession> findActiveByHash(String tokenHash) {
        List<RefreshSession> sessions = jdbc.query("""
                SELECT id, session_id, principal_type, user_id, guest_id, expires_at, revoked_at
                FROM refresh_sessions
                WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP(3)
                LIMIT 1
                """, this::map, tokenHash);
        return sessions.stream().findFirst();
    }

    public int revokeBySessionId(String sessionId) {
        return jdbc.update("UPDATE refresh_sessions SET revoked_at = CURRENT_TIMESTAMP(3) WHERE session_id = ? AND revoked_at IS NULL",
                sessionId);
    }

    private RefreshSession map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        return new RefreshSession(resultSet.getLong("id"), resultSet.getString("session_id"),
                PrincipalType.valueOf(resultSet.getString("principal_type")),
                resultSet.getObject("user_id", Long.class), resultSet.getString("guest_id"),
                resultSet.getTimestamp("expires_at").toInstant(), revokedAt == null ? null : revokedAt.toInstant());
    }
}
