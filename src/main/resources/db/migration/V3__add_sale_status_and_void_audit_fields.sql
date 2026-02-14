ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS voided_at            TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS voided_by_user_id    BIGINT,
    ADD COLUMN IF NOT EXISTS void_reason          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_by_user_id   BIGINT,
    ADD COLUMN IF NOT EXISTS posted_at            TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS posted_by_user_id    BIGINT,
    ADD COLUMN IF NOT EXISTS completed_at         TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS completed_by_user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_sales_status_date ON sales (status, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at);
CREATE INDEX IF NOT EXISTS idx_sales_posted_at ON sales (posted_at);
CREATE INDEX IF NOT EXISTS idx_sales_completed_at ON sales (completed_at);
CREATE INDEX IF NOT EXISTS idx_sales_voided_at ON sales (voided_at);
