ALTER TABLE order_items
ALTER COLUMN product_id TYPE VARCHAR(255)
USING product_id::text;