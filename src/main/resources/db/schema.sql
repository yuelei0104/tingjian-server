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
