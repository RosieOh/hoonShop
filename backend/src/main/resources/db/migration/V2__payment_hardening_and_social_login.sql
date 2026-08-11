-- =============================================================================
--  V2 — 결제 강화 + 소셜 로그인
-- =============================================================================

CREATE TABLE tbl_payment (
    id                BIGSERIAL PRIMARY KEY,

    -- 이중 결제를 막는 최후의 방어선. 애플리케이션이 뚫려도 DB가 막습니다.
    idempotency_key   VARCHAR(80)  NOT NULL,

    order_number      VARCHAR(24)  NOT NULL,
    customer_email    VARCHAR(120) NOT NULL,

    -- PG 결제창이 발급한 키. 카드번호가 아닙니다.
    payment_key       VARCHAR(200),
    method            VARCHAR(20),

    -- 서버가 계산한 청구 금액. 승인 응답과 대조하는 기준값입니다.
    requested_amount  BIGINT       NOT NULL CHECK (requested_amount > 0),
    approved_amount   BIGINT       NOT NULL DEFAULT 0 CHECK (approved_amount >= 0),
    cancelled_amount  BIGINT       NOT NULL DEFAULT 0 CHECK (cancelled_amount >= 0),

    status            VARCHAR(24)  NOT NULL,
    failure_code      VARCHAR(40),
    failure_message   VARCHAR(300),
    receipt_url       VARCHAR(300),

    -- 가상계좌: 승인 시점에 돈이 들어오지 않고, 입금 웹훅을 받아야 완료됩니다.
    va_bank           VARCHAR(40),
    va_number         VARCHAR(40),
    va_due_date       TIMESTAMPTZ,

    requested_at      TIMESTAMPTZ  NOT NULL,
    approved_at       TIMESTAMPTZ,
    version           BIGINT,

    CONSTRAINT uk_payment_idempotency UNIQUE (idempotency_key),
    -- 취소액이 승인액을 넘을 수 없습니다. 도메인에서도 막지만 DB에서도 막습니다.
    CONSTRAINT ck_payment_cancel_within_approved CHECK (cancelled_amount <= approved_amount)
);

CREATE INDEX idx_payment_order ON tbl_payment (order_number);
CREATE UNIQUE INDEX uk_payment_payment_key ON tbl_payment (payment_key)
    WHERE payment_key IS NOT NULL;
-- 대사 배치가 미확정 결제를 빠르게 찾도록
CREATE INDEX idx_payment_reconcile ON tbl_payment (status, requested_at)
    WHERE status IN ('UNKNOWN', 'REQUESTED');

-- 결제 원장: append-only. 상태가 바뀔 때마다 한 줄씩 쌓입니다.
CREATE TABLE tbl_payment_ledger (
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT       NOT NULL REFERENCES tbl_payment (id) ON DELETE CASCADE,
    entry_type  VARCHAR(30)  NOT NULL,
    detail      VARCHAR(300) NOT NULL,
    amount      BIGINT       NOT NULL DEFAULT 0,
    recorded_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_payment_ledger_payment ON tbl_payment_ledger (payment_id, recorded_at);

-- ------------------------------------------------------------- 소셜 로그인 ---
CREATE TABLE tbl_social_account (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES tbl_user (id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,

    -- 프로바이더가 발급한 불변 ID. 이메일이 아니라 이 값으로 계정을 식별합니다
    -- (이메일은 바뀔 수 있고, 카카오는 아예 안 줄 수도 있습니다).
    provider_user_id VARCHAR(100) NOT NULL,

    linked_email     VARCHAR(120),
    linked_at        TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ,

    CONSTRAINT uk_social_provider_user UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_social_account_user ON tbl_social_account (user_id);
