CREATE TABLE refresh_tokens
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    token_hash           VARCHAR(64)  NOT NULL UNIQUE,
    expires_at           TIMESTAMP    NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_used_at         TIMESTAMP    NULL,
    revoked_at           TIMESTAMP    NULL,
    replaced_by_token_id BIGINT       NULL,
    ip_address           VARCHAR(45)  NULL,
    user_agent           VARCHAR(255) NULL,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
