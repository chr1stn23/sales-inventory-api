ALTER TABLE sales
    DROP COLUMN IF EXISTS voided_at,
    DROP COLUMN IF EXISTS voided_by,
    DROP COLUMN IF EXISTS voided_by_user_id,
    DROP COLUMN IF EXISTS void_reason;