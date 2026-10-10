package com.xujiache.jingweitoolbox.tool;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {
    private final JdbcTemplate jdbc;

    public IdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<IdempotencyRecord> find(String principalKey, String toolId, String clientRequestId) {
        List<IdempotencyRecord> records = jdbc.query("""
                SELECT principal_key, tool_id, client_request_id, request_hash, state, response_json
                FROM idempotency_records
                WHERE principal_key = ? AND tool_id = ? AND client_request_id = ? AND expires_at > CURRENT_TIMESTAMP(3)
                LIMIT 1
                """, (resultSet, rowNum) -> new IdempotencyRecord(resultSet.getString("principal_key"),
                        resultSet.getString("tool_id"), resultSet.getString("client_request_id"),
                        resultSet.getString("request_hash"), resultSet.getString("state"), resultSet.getString("response_json")),
                principalKey, toolId, clientRequestId);
        return records.stream().findFirst();
    }

    public boolean insertPending(String principalKey, String toolId, String clientRequestId, String requestHash,
            Instant expiresAt) {
        try {
            jdbc.update("""
                    INSERT INTO idempotency_records(principal_key, tool_id, client_request_id, request_hash, state,
                        response_json, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'PENDING', NULL, ?, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                    """, principalKey, toolId, clientRequestId, requestHash, Timestamp.from(expiresAt));
            return true;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    public void complete(String principalKey, String toolId, String clientRequestId, String responseJson) {
        jdbc.update("""
                UPDATE idempotency_records SET state = 'COMPLETED', response_json = ?, updated_at = CURRENT_TIMESTAMP(3)
                WHERE principal_key = ? AND tool_id = ? AND client_request_id = ?
                """, responseJson, principalKey, toolId, clientRequestId);
    }

    public void delete(String principalKey, String toolId, String clientRequestId) {
        jdbc.update("DELETE FROM idempotency_records WHERE principal_key = ? AND tool_id = ? AND client_request_id = ?",
                principalKey, toolId, clientRequestId);
    }
}
