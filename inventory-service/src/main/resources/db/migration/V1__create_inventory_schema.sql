CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    stock        NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_stock_non_negative CHECK (stock >= 0)
);

CREATE INDEX idx_products_name ON products(name);
