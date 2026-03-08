-- V1__create_orders_table.sql

-- NOTE:
-- If you already enabled pgcrypto manually, you can keep this line.
-- If your DB user doesn't have permission, comment this out and enable extension manually once.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount > 0),

  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT chk_order_status
    CHECK (status IN (
      'CREATED',
      'PAYMENT_PENDING',
      'PAYMENT_COMPLETED',
      'PAYMENT_FAILED',
      'CANCELLED'
    ))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);