ALTER TABLE purchases
    DROP COLUMN IF EXISTS created_by;

ALTER TABLE purchases
    ADD COLUMN posted_by_user_id BIGINT,
    ADD COLUMN voided_by_user_id BIGINT,
    ADD COLUMN void_reason       VARCHAR(255);

ALTER TABLE purchases
    ADD CONSTRAINT fk_purchases_posted_by_user
        FOREIGN KEY (posted_by_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;

ALTER TABLE purchases
    ADD CONSTRAINT fk_purchases_voided_by_user
        FOREIGN KEY (voided_by_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;