-- V2__create_order_outbox_table.sql

CREATE TABLE IF NOT EXISTS order_outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,

  payload JSONB NOT NULL,

  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT chk_outbox_status
    CHECK (status IN ('NEW', 'PUBLISHED', 'FAILED'))
);

-- Indexes for publisher polling and aggregate lookups
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON order_outbox(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_id ON order_outbox(aggregate_id);