ALTER TABLE sales
    DROP COLUMN IF EXISTS deleted;

ALTER TABLE inventory_movements
    DROP COLUMN IF EXISTS deleted;

ALTER TABLE sale_status_history
    DROP COLUMN IF EXISTS deleted;