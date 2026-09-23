CREATE TABLE IF NOT EXISTS conversation (
    id CHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    title VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NULL,
    KEY idx_conversation_owner_started (owner_id, started_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversation_message (
    id CHAR(36) NOT NULL PRIMARY KEY,
    conversation_id CHAR(36) NOT NULL,
    speaker VARCHAR(16) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_conversation_message_order (conversation_id, created_at, id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS keyword_rule (
    id CHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    phrase VARCHAR(100) NOT NULL,
    vibration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 50,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_keyword_owner_phrase (owner_id, phrase),
    KEY idx_keyword_owner_priority (owner_id, priority, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS glossary_term (
    id CHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    term VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NULL,
    language VARCHAR(16) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority INT NOT NULL DEFAULT 50,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_glossary_owner_term_language (owner_id, term, language),
    KEY idx_glossary_owner_priority (owner_id, priority, term)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quick_phrase (
    id CHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    content VARCHAR(500) NOT NULL,
    category VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_quick_phrase_owner_content (owner_id, content),
    KEY idx_quick_phrase_owner_sort (owner_id, sort_order, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_user (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_session (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    access_expires_at DATETIME(3) NOT NULL,
    refresh_expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_auth_access_token (access_token_hash),
    UNIQUE KEY uq_auth_refresh_token (refresh_token_hash),
    KEY idx_auth_user_active (user_id, revoked_at, refresh_expires_at),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id)
        REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
