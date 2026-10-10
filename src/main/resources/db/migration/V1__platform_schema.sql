CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(128) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_identifier (identifier),
    KEY idx_users_status (status),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    principal_type VARCHAR(16) NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    guest_id VARCHAR(64) NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_session_id (session_id),
    KEY idx_refresh_expiry (expires_at),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_refresh_principal CHECK (principal_type IN ('GUEST', 'USER', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tools (
    tool_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    description VARCHAR(2000) NULL,
    status VARCHAR(16) NOT NULL,
    current_version VARCHAR(32) NOT NULL,
    input_schema JSON NULL,
    output_schema JSON NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    login_required TINYINT(1) NOT NULL DEFAULT 0,
    guest_limit BIGINT NULL,
    guest_limit_window_seconds BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (tool_id),
    KEY idx_tools_visibility (enabled, status, updated_at),
    CONSTRAINT chk_tools_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'DISABLED', 'DEPRECATED')),
    CONSTRAINT chk_tools_guest_limit CHECK (guest_limit IS NULL OR guest_limit >= 0),
    CONSTRAINT chk_tools_guest_window CHECK (guest_limit_window_seconds IS NULL OR guest_limit_window_seconds >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tool_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tool_id VARCHAR(64) NOT NULL,
    tool_version VARCHAR(32) NOT NULL,
    principal_type VARCHAR(16) NOT NULL,
    principal_id VARCHAR(128) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    input_hash CHAR(43) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tool_usage_tool_time (tool_id, created_at),
    KEY idx_tool_usage_principal (principal_type, principal_id, created_at),
    UNIQUE KEY uk_tool_usage_request (tool_id, principal_type, principal_id, client_request_id),
    CONSTRAINT fk_tool_usage_tool FOREIGN KEY (tool_id) REFERENCES tools(tool_id),
    CONSTRAINT chk_tool_usage_principal CHECK (principal_type IN ('GUEST', 'USER', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE idempotency_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    principal_key VARCHAR(160) NOT NULL,
    tool_id VARCHAR(64) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    request_hash CHAR(43) NOT NULL,
    state VARCHAR(16) NOT NULL,
    response_json JSON NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_request (principal_key, tool_id, client_request_id),
    KEY idx_idempotency_expiry (expires_at),
    CONSTRAINT fk_idempotency_tool FOREIGN KEY (tool_id) REFERENCES tools(tool_id),
    CONSTRAINT chk_idempotency_state CHECK (state IN ('PENDING', 'COMPLETED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_type VARCHAR(16) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    details_json JSON NULL,
    request_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_resource (resource_type, resource_id, created_at),
    KEY idx_audit_actor (actor_type, actor_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
