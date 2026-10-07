CREATE TABLE IF NOT EXISTS usage_user_plan (
    user_id VARCHAR(64) NOT NULL PRIMARY KEY,
    plan_code VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS usage_daily_bucket (
    user_id VARCHAR(64) NOT NULL,
    usage_date DATE NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    used_amount BIGINT NOT NULL DEFAULT 0,
    reserved_amount BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, usage_date, metric_code),
    CONSTRAINT chk_usage_bucket_used CHECK (used_amount >= 0),
    CONSTRAINT chk_usage_bucket_reserved CHECK (reserved_amount >= 0)
);

CREATE TABLE IF NOT EXISTS usage_reservation (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    usage_date DATE NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_usage_reservation_idempotency UNIQUE (user_id, idempotency_key),
    INDEX idx_usage_reservation_expiry (status, expires_at),
    INDEX idx_usage_reservation_user_day (user_id, usage_date)
);
