-- =============================================================================
--  hoonshop 초기 스키마
--  스키마의 주인은 Flyway입니다. 엔티티를 고치면 반드시 새 마이그레이션을 추가하세요
--  (ddl-auto=validate라 불일치가 있으면 애플리케이션이 기동하지 않습니다).
-- =============================================================================

-- ---------------------------------------------------------------- identity ---
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(40)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    grade         VARCHAR(20)  NOT NULL,
    point         INTEGER      NOT NULL DEFAULT 0,
    joined_at     TIMESTAMPTZ  NOT NULL
);

-- ----------------------------------------------------------------- catalog ---
CREATE TABLE product (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(16)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    colorway      VARCHAR(32)  NOT NULL,
    description   VARCHAR(500) NOT NULL,
    list_price    BIGINT       NOT NULL CHECK (list_price >= 0),
    discount_rate INTEGER      NOT NULL DEFAULT 0 CHECK (discount_rate >= 0 AND discount_rate < 100),
    rating        DOUBLE PRECISION NOT NULL DEFAULT 0,
    review_count  INTEGER      NOT NULL DEFAULT 0,
    sold_count    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_created_at ON product (created_at DESC);

CREATE TABLE product_palette (
    product_id BIGINT     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    hex        VARCHAR(7) NOT NULL,
    line_order INTEGER    NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE product_material (
    product_id BIGINT      NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    material   VARCHAR(60) NOT NULL,
    line_order INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE product_size (
    product_id BIGINT      NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    size_label VARCHAR(40) NOT NULL,
    line_order INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE product_color_option (
    product_id  BIGINT      NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    color_id    VARCHAR(32) NOT NULL,
    color_label VARCHAR(32) NOT NULL,
    color_hex   VARCHAR(7)  NOT NULL,
    line_order  INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);
CREATE INDEX idx_color_option_color_id ON product_color_option (color_id);

CREATE TABLE product_badge (
    product_id BIGINT      NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    badge      VARCHAR(20) NOT NULL,
    PRIMARY KEY (product_id, badge)
);

-- 재고는 상품과 별도 애그리거트입니다 (잠금 범위를 좁히기 위해).
CREATE TABLE inventory (
    id       BIGSERIAL PRIMARY KEY,
    code     VARCHAR(16) NOT NULL UNIQUE,
    quantity INTEGER     NOT NULL CHECK (quantity >= 0),
    version  BIGINT
);
CREATE INDEX idx_inventory_quantity ON inventory (quantity);

-- --------------------------------------------------------------- promotion ---
CREATE TABLE coupon (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40) NOT NULL UNIQUE,
    name         VARCHAR(80) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    value        INTEGER     NOT NULL,
    min_amount   BIGINT      NOT NULL DEFAULT 0,
    max_discount BIGINT      NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    stackable    BOOLEAN     NOT NULL DEFAULT FALSE
);

-- ------------------------------------------------------------------- order ---
-- 주문번호 채번. 애플리케이션에서 MAX+1을 계산하면 동시 주문 시 충돌합니다.
CREATE SEQUENCE order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    order_number      VARCHAR(24)  NOT NULL UNIQUE,
    customer_email    VARCHAR(120) NOT NULL,
    customer_name     VARCHAR(40)  NOT NULL,

    -- 배송지는 주소록을 참조하지 않고 복사합니다 (주문 시점 기록 보존).
    recipient         VARCHAR(40)  NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    zipcode           VARCHAR(10)  NOT NULL,
    address1          VARCHAR(200) NOT NULL,
    address2          VARCHAR(200),
    delivery_memo     VARCHAR(100),

    -- 최종 금액만이 아니라 계산 과정을 모두 보존합니다.
    items_list_total  BIGINT       NOT NULL,
    item_discount     BIGINT       NOT NULL DEFAULT 0,
    coupon_discount   BIGINT       NOT NULL DEFAULT 0,
    shipping_fee      BIGINT       NOT NULL DEFAULT 0,
    shipping_discount BIGINT       NOT NULL DEFAULT 0,
    payable           BIGINT       NOT NULL,

    status            VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    paid_at           TIMESTAMPTZ,
    version           BIGINT
);
CREATE INDEX idx_orders_customer ON orders (customer_email, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status, created_at DESC);

CREATE TABLE order_line (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_code VARCHAR(16)  NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    color_id     VARCHAR(32)  NOT NULL,
    color_label  VARCHAR(32)  NOT NULL,
    size_label   VARCHAR(40),
    quantity     INTEGER      NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    -- 주문 시점 가격을 박제합니다. 상품 가격이 바뀌어도 영수증은 그대로여야 합니다.
    list_price   BIGINT       NOT NULL,
    unit_price   BIGINT       NOT NULL
);
CREATE INDEX idx_order_line_order ON order_line (order_id);

CREATE TABLE order_coupon (
    order_id    BIGINT      NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    coupon_code VARCHAR(40) NOT NULL
);
CREATE INDEX idx_order_coupon_order ON order_coupon (order_id);

-- ----------------------------------------------------------------- payment ---
CREATE TABLE payment (
    id              BIGSERIAL PRIMARY KEY,
    -- 이 UNIQUE 제약이 이중 결제를 막는 최후의 방어선입니다.
    idempotency_key VARCHAR(80) NOT NULL,
    order_number    VARCHAR(24) NOT NULL,
    payment_key     VARCHAR(60),
    method          VARCHAR(20) NOT NULL,
    amount          BIGINT      NOT NULL CHECK (amount >= 0),
    status          VARCHAR(20) NOT NULL,
    failure_code    VARCHAR(40),
    failure_message VARCHAR(200),
    requested_at    TIMESTAMPTZ NOT NULL,
    approved_at     TIMESTAMPTZ,
    CONSTRAINT uk_payment_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_payment_order ON payment (order_number);
