CREATE TABLE IF NOT EXISTS processed_events (
    event_id      VARCHAR(80) PRIMARY KEY,
    consumer      VARCHAR(100) NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_consumer
ON processed_events(consumer);