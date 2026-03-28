-- V1__create_orders_schema.sql
-- Flyway ejecuta esto automáticamente al arrancar la app si no fue aplicado antes.
-- El nombre del archivo define la versión: V1 < V2 < V3...
-- Una vez aplicada, NUNCA modificar este archivo — crear V2 con los cambios.

-- ── Tabla principal de pedidos ────────────────────────────────────────────────
CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT        NOT NULL,
    contact_email  VARCHAR(100)  NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    total_amount   NUMERIC(12,2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (
        status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')
    ),
    CONSTRAINT chk_total_positive CHECK (total_amount >= 0)
);

-- ── Tabla de ítems ────────────────────────────────────────────────────────────
CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   VARCHAR(100)  NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    quantity     INTEGER       NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,

    CONSTRAINT chk_quantity_positive   CHECK (quantity > 0),
    CONSTRAINT chk_unit_price_positive CHECK (unit_price > 0)
);

-- ── Índices ───────────────────────────────────────────────────────────────────
-- B-tree: para buscar pedidos de un cliente (query más frecuente)
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

-- B-tree: para filtrar por status (ej: encontrar todos los PENDING)
CREATE INDEX idx_orders_status ON orders(status);

-- Compuesto: para la query findByCustomerAndStatus — el orden importa
-- Sirve para: WHERE customer_id = ? AND status = ?
-- Sirve para: WHERE customer_id = ?   (solo el primer campo)
-- NO sirve para: WHERE status = ?     (solo el segundo campo)
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);

-- Covering index: permite Index-Only Scan para el listado de pedidos
-- Query: SELECT id, total_amount, status FROM orders WHERE customer_id = ?
-- Con este índice, Postgres lee solo el índice, no va a la tabla → más rápido
CREATE INDEX idx_orders_customer_covering
    ON orders(customer_id)
    INCLUDE (total_amount, status, created_at);

-- FK index: acelera los joins y el CASCADE DELETE
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
