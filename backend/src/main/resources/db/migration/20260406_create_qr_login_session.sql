CREATE TABLE IF NOT EXISTS auth_qr_login_session
(
    id BIGSERIAL PRIMARY KEY,
    qr_token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT,
    status VARCHAR(20) NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    scanned_time TIMESTAMP,
    confirmed_time TIMESTAMP,
    consumed_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_qr_login_session_token
ON auth_qr_login_session(qr_token);

CREATE INDEX idx_auth_qr_login_session_status
ON auth_qr_login_session(status);
