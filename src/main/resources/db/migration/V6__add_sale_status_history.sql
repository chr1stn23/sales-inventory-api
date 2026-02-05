CREATE TABLE sale_status_history
(
    id                 BIGSERIAL PRIMARY KEY,
    sale_id            BIGINT       NOT NULL,
    from_status        VARCHAR(30)  NOT NULL,
    to_status          VARCHAR(30)  NOT NULL,
    changed_at         TIMESTAMP    NOT NULL,
    changed_by_user_id BIGINT       NULL,
    reason             VARCHAR(255) NULL,
    created_at         TIMESTAMP    NULL,
    updated_at         TIMESTAMP    NULL,
    deleted            BOOLEAN      NOT NULL
);

CREATE INDEX idx_sale_status_history_sale ON sale_status_history (sale_id);
CREATE INDEX idx_sale_status_history_changed_at ON sale_status_history (changed_at);

ALTER TABLE sale_status_history
    ADD CONSTRAINT fk_sale_status_history_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id);