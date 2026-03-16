ALTER TABLE processed_events
ADD COLUMN IF NOT EXISTS event_type VARCHAR(80),
ADD COLUMN IF NOT EXISTS order_id UUID;

CREATE INDEX IF NOT EXISTS idx_processed_events_order_id
ON processed_events(order_id);