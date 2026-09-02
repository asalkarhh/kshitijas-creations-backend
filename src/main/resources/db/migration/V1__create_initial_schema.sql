CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_admin_users_email UNIQUE (email),
    CONSTRAINT chk_admin_users_role CHECK (role IN ('ADMIN'))
);

CREATE INDEX idx_admin_users_active ON admin_users (active);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT chk_categories_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_categories_active_display_order
    ON categories (active, display_order);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    product_code VARCHAR(80) NOT NULL,
    short_description VARCHAR(500),
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    instagram_url VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT uk_products_product_code UNIQUE (product_code),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_products_category_available_deleted
    ON products (category_id, available, deleted);
CREATE INDEX idx_products_featured_available_deleted
    ON products (featured, available, deleted);
CREATE INDEX idx_products_display_order ON products (display_order);

CREATE TABLE product_media (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    thumbnail_key VARCHAR(500),
    alt_text VARCHAR(300),
    display_order INTEGER NOT NULL DEFAULT 0,
    primary_media BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_product_media_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_product_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT chk_product_media_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_product_media_product_display_order
    ON product_media (product_id, display_order);
CREATE INDEX idx_product_media_product_primary
    ON product_media (product_id, primary_media);

CREATE TABLE banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500),
    image_key VARCHAR(500) NOT NULL,
    mobile_image_key VARCHAR(500),
    button_text VARCHAR(100),
    target_url VARCHAR(500),
    start_at TIMESTAMP WITH TIME ZONE,
    end_at TIMESTAMP WITH TIME ZONE,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_banners_date_range CHECK (end_at IS NULL OR start_at IS NULL OR end_at > start_at),
    CONSTRAINT chk_banners_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_banners_active_schedule_display_order
    ON banners (active, start_at, end_at, display_order);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    customer_note VARCHAR(1000),
    subtotal NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    whatsapp_opened BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_status CHECK (status IN ('PLACED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);
CREATE INDEX idx_orders_mobile_number ON orders (mobile_number);
CREATE INDEX idx_orders_created_at ON orders (created_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_code VARCHAR(80) NOT NULL,
    product_image_key VARCHAR(500),
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_line_total CHECK (line_total >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
