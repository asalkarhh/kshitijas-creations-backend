CREATE TABLE IF NOT EXISTS order_product_reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    rating NUMERIC(2,1) NOT NULL,
    review_text VARCHAR(2000),
    reviewer_name VARCHAR(200) NOT NULL,
    reviewer_city VARCHAR(120) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_product_review UNIQUE (order_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_order_product_reviews_product_published ON order_product_reviews(product_id, published);
