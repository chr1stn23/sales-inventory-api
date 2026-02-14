ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(40) NOT NULL DEFAULT 'SALE_OUT';

CREATE UNIQUE INDEX IF NOT EXISTS ux_inventory_movement_source_event
    ON inventory_movements (source_type, source_id, event_type);
