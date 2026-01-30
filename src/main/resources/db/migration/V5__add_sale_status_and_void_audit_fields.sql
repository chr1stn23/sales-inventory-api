ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS status            VARCHAR(20),
    ADD COLUMN IF NOT EXISTS voided_at         TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS voided_by         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS voided_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS void_reason       VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_sales_status_date ON sales (status, sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_voided_at ON sales (voided_at);
CREATE INDEX IF NOT EXISTS idx_sales_voided_by ON sales (voided_by);


UPDATE sales
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE sales
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_status_date ON sales (status, sale_date);

