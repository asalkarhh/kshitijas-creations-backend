ALTER TABLE orders ADD COLUMN IF NOT EXISTS viewed BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark existing non-placed orders as already viewed
UPDATE orders SET viewed = TRUE WHERE status != 'PLACED';

CREATE INDEX IF NOT EXISTS idx_orders_viewed ON orders (viewed);

