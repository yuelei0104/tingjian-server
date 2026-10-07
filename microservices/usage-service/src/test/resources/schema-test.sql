DROP TABLE IF EXISTS usage_reservation;
DROP TABLE IF EXISTS usage_daily_bucket;
DROP TABLE IF EXISTS usage_user_plan;

CREATE TABLE usage_user_plan (
    user_id VARCHAR(64) NOT NULL PRIMARY KEY,
    plan_code VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usage_daily_bucket (
    user_id VARCHAR(64) NOT NULL,
    usage_date DATE NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    used_amount BIGINT NOT NULL DEFAULT 0,
    reserved_amount BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, usage_date, metric_code)
);

CREATE TABLE usage_reservation (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    usage_date DATE NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usage_reservation_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_usage_reservation_expiry
    ON usage_reservation(status, expires_at);
CREATE INDEX idx_usage_reservation_user_day
    ON usage_reservation(user_id, usage_date);
