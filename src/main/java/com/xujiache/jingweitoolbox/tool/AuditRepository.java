package com.xujiache.jingweitoolbox.tool;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String actorType, String actorId, String action, String resourceType, String resourceId,
            String detailsJson, String requestId) {
        jdbc.update("""
                INSERT INTO audit_logs(actor_type, actor_id, action, resource_type, resource_id, details_json,
                    request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, actorType, actorId, action, resourceType, resourceId, detailsJson, requestId, Timestamp.from(Instant.now()));
    }
}
