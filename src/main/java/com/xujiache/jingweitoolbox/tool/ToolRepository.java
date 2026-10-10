package com.xujiache.jingweitoolbox.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ToolRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ToolRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<Tool> findById(String toolId) {
        List<Tool> tools = jdbc.query(selectSql() + " WHERE tool_id = ? LIMIT 1", this::map, toolId);
        return tools.stream().findFirst();
    }

    public List<Tool> listPublic(ToolStatus status, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(selectSql()).append(" WHERE enabled = 1 AND status = ?");
        List<Object> args = new ArrayList<>();
        args.add(status == null ? ToolStatus.PUBLISHED.name() : status.name());
        sql.append(" ORDER BY updated_at DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add((long) (page - 1) * pageSize);
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public List<Tool> listAll(ToolStatus status, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(selectSql());
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append(" WHERE status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY updated_at DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add((long) (page - 1) * pageSize);
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public long countPublic(ToolStatus status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tools WHERE enabled = 1 AND status = ?",
                Long.class, status == null ? ToolStatus.PUBLISHED.name() : status.name());
    }

    public long countAll(ToolStatus status) {
        if (status == null) {
            return jdbc.queryForObject("SELECT COUNT(*) FROM tools", Long.class);
        }
        return jdbc.queryForObject("SELECT COUNT(*) FROM tools WHERE status = ?", Long.class, status.name());
    }

    public Tool insert(ToolUpsertRequest request) {
        jdbc.update("""
                INSERT INTO tools(tool_id, display_name, description, status, current_version, input_schema, output_schema,
                    enabled, login_required, guest_limit, guest_limit_window_seconds, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, NULL, NULL, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                """, request.toolId(), request.displayName(), request.description(), request.status().name(),
                request.currentVersion(), json(request.inputSchema()), json(request.outputSchema()));
        return findById(request.toolId()).orElseThrow();
    }

    public Optional<Tool> updateMetadata(String toolId, ToolPatchRequest request) {
        int updated = jdbc.update("""
                UPDATE tools SET display_name = COALESCE(?, display_name), description = COALESCE(?, description),
                    current_version = COALESCE(?, current_version), status = COALESCE(?, status),
                    input_schema = COALESCE(?, input_schema), output_schema = COALESCE(?, output_schema),
                    version = version + 1, updated_at = CURRENT_TIMESTAMP(3)
                WHERE tool_id = ? AND version = ?
                """, request.displayName(), request.description(), request.currentVersion(),
                request.status() == null ? null : request.status().name(), json(request.inputSchema()), json(request.outputSchema()),
                toolId, request.expectedVersion());
        return updated == 0 ? Optional.empty() : findById(toolId);
    }

    public Optional<Tool> updatePolicy(String toolId, ToolPolicyPatch request, Long guestLimit, Long windowSeconds) {
        int updated = jdbc.update("""
                UPDATE tools SET enabled = COALESCE(?, enabled), login_required = COALESCE(?, login_required),
                    guest_limit = ?, guest_limit_window_seconds = ?, version = version + 1,
                    updated_at = CURRENT_TIMESTAMP(3)
                WHERE tool_id = ? AND version = ?
                """, request.enabled(), request.loginRequired(), guestLimit, windowSeconds, toolId, request.expectedVersion());
        return updated == 0 ? Optional.empty() : findById(toolId);
    }

    public UsageCounts usage(String toolId, java.time.Instant from, java.time.Instant to) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                    SUM(CASE WHEN principal_type = 'GUEST' THEN 1 ELSE 0 END) AS guests,
                    SUM(CASE WHEN principal_type IN ('USER', 'ADMIN') THEN 1 ELSE 0 END) AS users
                FROM tool_usage WHERE tool_id = ? AND created_at >= ? AND created_at < ?
                """, (resultSet, rowNum) -> new UsageCounts(resultSet.getLong("total"), resultSet.getLong("guests"),
                        resultSet.getLong("users")), toolId, Timestamp.from(from), Timestamp.from(to));
    }

    public void recordUsage(String toolId, String toolVersion, String principalType, String principalId,
            String clientRequestId, String inputHash, String outcome) {
        jdbc.update("""
                INSERT INTO tool_usage(tool_id, tool_version, principal_type, principal_id, client_request_id,
                    input_hash, outcome, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))
                """, toolId, toolVersion, principalType, principalId, clientRequestId, inputHash, outcome);
    }

    private String selectSql() {
        return """
                SELECT tool_id, display_name, description, status, current_version, input_schema, output_schema,
                    enabled, login_required, guest_limit, guest_limit_window_seconds, version, updated_at
                FROM tools
                """;
    }

    private Tool map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        Long guestLimit = resultSet.getObject("guest_limit", Long.class);
        Long windowSeconds = resultSet.getObject("guest_limit_window_seconds", Long.class);
        return new Tool(resultSet.getString("tool_id"), resultSet.getString("display_name"),
                resultSet.getString("description"), ToolStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("current_version"), parseJson(resultSet.getString("input_schema")),
                parseJson(resultSet.getString("output_schema")),
                new ToolPolicy(resultSet.getBoolean("enabled"), resultSet.getBoolean("login_required"), guestLimit,
                        windowSeconds), resultSet.getLong("version"), resultSet.getTimestamp("updated_at").toInstant());
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("工具 Schema 不是有效 JSON", exception);
        }
    }

    private java.util.Map<String, Object> parseJson(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据库中的工具 Schema 无法解析", exception);
        }
    }

    public record UsageCounts(long total, long guests, long users) {
    }
}
