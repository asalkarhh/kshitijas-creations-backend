UPDATE orders o
SET review_product_id = (
    SELECT oi.product_id
    FROM order_items oi
    WHERE oi.order_id = o.id
    ORDER BY oi.id
    LIMIT 1
)
WHERE o.review_used = TRUE
  AND o.review_product_id IS NULL
  AND (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = o.id) = 1;

UPDATE orders
SET reviewer_name = customer_name,
    reviewer_city = city
WHERE review_used = TRUE
  AND (reviewer_name IS NULL OR reviewer_city IS NULL);
