-- =============================================================================
--  hoonshop 초기 스키마
--
--  테이블 이름은 모두 tbl_ 접두사를 씁니다.
--  덕분에 예약어 회피용 이름(user → app_user, order → orders)이 필요 없어져
--  tbl_user / tbl_order 처럼 도메인 용어 그대로 쓸 수 있습니다.
--
--  스키마의 주인은 Flyway입니다. 엔티티를 고치면 반드시 새 마이그레이션을 추가하세요
--  (ddl-auto=validate라 불일치가 있으면 애플리케이션이 기동하지 않습니다).
-- =============================================================================

-- ---------------------------------------------------------------- identity ---
CREATE TABLE tbl_user (
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
CREATE TABLE tbl_product (
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
CREATE INDEX idx_product_category ON tbl_product (category);
CREATE INDEX idx_product_created_at ON tbl_product (created_at DESC);

CREATE TABLE tbl_product_palette (
    product_id BIGINT     NOT NULL REFERENCES tbl_product (id) ON DELETE CASCADE,
    hex        VARCHAR(7) NOT NULL,
    line_order INTEGER    NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE tbl_product_material (
    product_id BIGINT      NOT NULL REFERENCES tbl_product (id) ON DELETE CASCADE,
    material   VARCHAR(60) NOT NULL,
    line_order INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE tbl_product_size (
    product_id BIGINT      NOT NULL REFERENCES tbl_product (id) ON DELETE CASCADE,
    size_label VARCHAR(40) NOT NULL,
    line_order INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);

CREATE TABLE tbl_product_color_option (
    product_id  BIGINT      NOT NULL REFERENCES tbl_product (id) ON DELETE CASCADE,
    color_id    VARCHAR(32) NOT NULL,
    color_label VARCHAR(32) NOT NULL,
    color_hex   VARCHAR(7)  NOT NULL,
    line_order  INTEGER     NOT NULL,
    PRIMARY KEY (product_id, line_order)
);
CREATE INDEX idx_color_option_color_id ON tbl_product_color_option (color_id);

CREATE TABLE tbl_product_badge (
    product_id BIGINT      NOT NULL REFERENCES tbl_product (id) ON DELETE CASCADE,
    badge      VARCHAR(20) NOT NULL,
    PRIMARY KEY (product_id, badge)
);

-- 재고는 상품과 별도 애그리거트입니다 (잠금 범위를 좁히기 위해).
CREATE TABLE tbl_inventory (
    id       BIGSERIAL PRIMARY KEY,
    code     VARCHAR(16) NOT NULL UNIQUE,
    quantity INTEGER     NOT NULL CHECK (quantity >= 0),
    version  BIGINT
);
CREATE INDEX idx_inventory_quantity ON tbl_inventory (quantity);

-- --------------------------------------------------------------- promotion ---
CREATE TABLE tbl_coupon (
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

CREATE TABLE tbl_order (
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
CREATE INDEX idx_order_customer ON tbl_order (customer_email, created_at DESC);
CREATE INDEX idx_order_status ON tbl_order (status, created_at DESC);

CREATE TABLE tbl_order_line (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES tbl_order (id) ON DELETE CASCADE,
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
CREATE INDEX idx_order_line_order ON tbl_order_line (order_id);

CREATE TABLE tbl_order_coupon (
    order_id    BIGINT      NOT NULL REFERENCES tbl_order (id) ON DELETE CASCADE,
    coupon_code VARCHAR(40) NOT NULL
);
CREATE INDEX idx_order_coupon_order ON tbl_order_coupon (order_id);
