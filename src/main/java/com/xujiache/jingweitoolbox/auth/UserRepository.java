package com.xujiache.jingweitoolbox.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByIdentifier(String identifier) {
        List<UserAccount> users = jdbc.query("""
                SELECT id, identifier, password_hash, display_name, role, status, version
                FROM users WHERE identifier = ? LIMIT 1
                """, this::map, identifier);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findById(long id) {
        List<UserAccount> users = jdbc.query("""
                SELECT id, identifier, password_hash, display_name, role, status, version
                FROM users WHERE id = ? LIMIT 1
                """, this::map, id);
        return users.stream().findFirst();
    }

    public UserAccount insert(String identifier, String passwordHash, String displayName, UserRole role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users(identifier, password_hash, display_name, role, status, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, identifier);
            statement.setString(2, passwordHash);
            statement.setString(3, displayName);
            statement.setString(4, role.name());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建用户后未返回 ID");
        }
        return findById(key.longValue()).orElseThrow();
    }

    public int updatePassword(long id, String passwordHash) {
        return jdbc.update("UPDATE users SET password_hash = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?",
                passwordHash, id);
    }

    private UserAccount map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new UserAccount(resultSet.getLong("id"), resultSet.getString("identifier"),
                resultSet.getString("password_hash"), resultSet.getString("display_name"),
                UserRole.valueOf(resultSet.getString("role")), UserStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("version"));
    }
}
