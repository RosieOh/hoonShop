# hoonshop API

비즈 액세서리 쇼핑몰 백엔드. **Spring Boot 3 + Java 17**, DDD 전술 패턴으로 구성했습니다.

```bash
docker compose up -d      # PostgreSQL (호스트 5433 포트)
./gradlew bootRun         # http://localhost:8080
./gradlew test            # 도메인 단위 테스트 + 아키텍처 검사
```

- API 문서 — http://localhost:8080/swagger-ui.html
- 데모 계정 — `hoon@example.com` / `admin@hoonshop.com`, 비밀번호 공통 `hoonshop`
- 결제 거절 재현 — 카드번호 끝 4자리를 `0000`으로

> PostgreSQL을 5432가 아니라 **5433**으로 내보냅니다. 로컬에 이미 설치된 PostgreSQL을
> 건드리지 않기 위해서입니다. 5432가 비어 있다면 `docker-compose.yml`과
> `application.yml`의 포트를 바꿔도 됩니다.

---

## 바운디드 컨텍스트

각 컨텍스트는 `domain → application → infrastructure / presentation` 4계층입니다.

| 컨텍스트 | 애그리거트 | 핵심 규칙 |
|---|---|---|
| `catalog` | `Product`, `Inventory` | 판매가 파생 계산, 옵션 검증, 재고 차감 |
| `identity` | `User` | 비밀번호 검증, JWT 발급, 역할 |
| `promotion` | `Coupon` | 사용 가능 판정, 할인액, 조합 규칙 |
| `order` | `Order` (+ `OrderLine`) | 금액 확정, 상태 전이 |
| `payment` | `Payment` | 멱등성, PG 승인 |
| `admin` | — | 여러 컨텍스트를 가로지르는 **읽기 전용** 리포팅 |

```
com.hoonshop
├─ common/           Money · AggregateRoot · DomainEvent · DomainException
│                    config(Security·CORS·시드) · presentation(예외 → HTTP 변환)
├─ catalog/          Product · Inventory · ProductCode · ColorOption
├─ identity/         User · Email · Role · PasswordEncoder(포트)
├─ promotion/        Coupon · CouponDiscountPolicy(도메인 서비스)
├─ order/            Order · OrderLine · OrderAmounts · ShippingAddress · ShippingPolicy
├─ payment/          Payment · PaymentGateway(포트)
└─ admin/            AdminStatsService
```

---

## 설계 판단과 근거

### 1. 재고를 상품에서 떼어냈다

`Product`와 `Inventory`는 별개 애그리거트입니다. 한 덩어리로 묶으면 재고를 뺄 때마다
상품 행에 락이 걸려 **카탈로그 조회까지 줄을 섭니다.** 애그리거트 경계는 "함께 지켜야 하는
불변식"으로 정하는 것이지 "같은 화면에 보이는 것"으로 정하는 게 아닙니다.

화면은 둘 다 필요하므로 **애플리케이션 계층에서 조합**합니다
([`ProductQueryService.combine()`](src/main/java/com/hoonshop/catalog/application/ProductQueryService.java) —
코드를 모아 한 번에 조회해 N+1도 피합니다).

### 2. 금액은 클라이언트에서 받지 않는다

`Order.place()`는 **결제 금액을 인자로 받지 않습니다.** 항목과 쿠폰 코드만 받고 총액은
스스로 계산합니다. 요청의 `amount`는 마지막에 `assertPayableMatches()`로 대조만 하고,
다르면 조용히 덮어쓰지 않고 **409로 막습니다** — 덮어쓰면 프론트 버그를 영영 못 찾습니다.

```
검증됨: amount=1000으로 조작 요청 → 409 AMOUNT_MISMATCH
        "서버 30,200원 / 요청 1,000원"
```

### 3. 가격은 주문에 복사한다

`OrderLine`은 상품 코드만이 아니라 **주문 시점의 정가와 판매가를 함께 박제**합니다.
코드만 들고 있다가 나중에 조회하면 판매가가 바뀐 순간 과거 주문의 결제 금액이 소급해서
달라집니다. 영수증은 그때 그 값이어야 합니다.

`OrderAmounts`도 최종 금액 하나가 아니라 상품합계·상품할인·쿠폰할인·배송비·배송비할인을
모두 남깁니다. "왜 이 금액이냐"를 재계산으로 답하면 정책이 바뀐 뒤엔 다른 답이 나옵니다.

### 4. 재고는 결제 승인 시점에, 락을 걸고 뺀다

주문서 작성 시점에 선점하면 결제를 포기한 장바구니가 재고를 물고 있게 되고, 반환 타이머와
만료 처리가 줄줄이 필요해집니다. 동시성은 두 겹으로 막습니다.

- **비관적 락** (`SELECT ... FOR UPDATE`) — 같은 상품 동시 결제 시 뒤 트랜잭션이 대기
- **`@Version` 낙관적 락** — 락을 빠뜨린 경로가 새로 생겨도 갱신 분실을 잡아냄

여러 상품을 뺄 때는 **상품 코드 순으로 정렬**해 락을 잡습니다. A→B와 B→A가 만나면
데드락이 나기 때문입니다.

### 5. 멱등키로 이중 결제를 막는다

`payment.idempotency_key`에 UNIQUE 제약이 걸려 있습니다. 네트워크가 끊겨 재시도되면
애플리케이션이 기존 승인 결과를 그대로 돌려주고, 애플리케이션이 뚫려도 DB가 막습니다.

```
검증됨: 같은 Idempotency-Key로 재요청 → 같은 paymentKey 반환, 재고 변화 없음
```

### 6. 컨텍스트는 이벤트로 잇는다

```
ConfirmPaymentService
  └─ PaymentApproved  →  order.infrastructure.PaymentEventListener
                            └─ Order.markPaid()
                                 └─ OrderPaid  →  catalog.infrastructure.OrderEventListener
                                                    └─ Inventory.deduct()
```

`payment`가 `catalog`를 직접 호출하지 않습니다. 나중에 "결제 완료 시 포인트 적립",
"품절 시 알림" 같은 처리를 추가할 때 **구독자만 늘리면 됩니다.**

구독자를 `infrastructure`에 둔 이유: 다른 컨텍스트의 타입을 아는 것은 어댑터의 일입니다.
도메인은 자기 이벤트만 압니다 (ArchUnit이 강제).

**`@TransactionalEventListener(AFTER_COMMIT)`이 아니라 일반 `@EventListener`**입니다.
커밋 이후로 미루면 "결제는 승인됐는데 재고는 안 빠진" 창이 생기고, 되돌리려면 보상
트랜잭션이 필요해집니다. 단일 DB를 쓰는 지금 규모에서는 한 트랜잭션이 정답입니다.

### 7. 컨텍스트 간 호출은 부패 방지 계층을 거친다

`order`가 `catalog.Product`를 직접 들고 다니면 카탈로그 모델이 바뀔 때마다 주문이 흔들립니다.
주문이 상품에 대해 알아야 하는 건 네 가지뿐입니다 — 이름, 정가, 판매가, 옵션 유효성.
딱 그만큼만 [`ProductCatalogPort`](src/main/java/com/hoonshop/order/application/port/ProductCatalogPort.java)로
통과시키고, 구현은 `order.infrastructure`에 둡니다.

### 8. 도메인은 JPA는 알고 Spring은 모른다

순수 도메인 모델 + 별도 영속성 모델 + 매퍼가 이론적으로 더 깨끗하지만, 이 규모에서는
매퍼 유지비가 이득을 넘어섭니다. **의도한 절충**이고, 대신 이것들은 테스트로 강제합니다
([`LayerDependencyTest`](src/test/java/com/hoonshop/architecture/LayerDependencyTest.java)):

- 도메인 → 인프라/프레젠테이션/애플리케이션 의존 금지
- 도메인 → `org.springframework..` 의존 금지 (순수 자바로 테스트 가능해야 함)
- 컨텍스트 도메인 → 다른 컨텍스트 의존 금지 (공유 커널 제외)
- 프레젠테이션 → `*JpaRepository` 직접 사용 금지

문서에 적어두는 것만으로는 안 지켜집니다. 급할 때 누군가 도메인에 `@Autowired`를 붙이고,
리뷰에서 놓치고, 6개월 뒤엔 도메인이 Spring 없이 테스트조차 안 됩니다.

### 9. 원화는 `long`, `Money` 값 객체로

`double`은 부동소수점 오차로 금액에 쓸 수 없고, `BigDecimal`은 원화에 없는 소수부 때문에
스케일·반올림 규칙을 매번 신경 써야 합니다. `Money`는 음수를 허용하지 않고, 뺄셈이 음수가
되면 0으로 자르지 않고 **예외를 던집니다** — 조용히 0이 되면 잘못된 청구를 못 찾습니다.

---

## 검증 상태

```
./gradlew test          54개 통과 (도메인 단위 45 + 아키텍처 6 + Money 3)
API 통합 시나리오        42개 통과 (실제 PostgreSQL 대상)
```

API 검증에 포함된 것: 필터·정렬·검색, 로그인/리프레시(토큰 타입 검증 포함), 쿠폰 사용
불가 사유, 재고 사전 확인, **금액 조작 차단**, 옵션 위조 차단, **멱등 결제**, 이벤트
연쇄로 인한 재고 차감, 상태 전이 규칙, 권한(403), 취소 시 재고 복원.

---

## 프론트엔드 연동

`src/api/baseQuery.js`의 `baseUrl`을 `http://localhost:8080/api`로 바꾸면 됩니다.
MSW는 `src/main.jsx`에서 `startMockServer()` 호출만 지우면 꺼집니다.

**한 가지 차이**: 목 서버는 비로그인 주문을 허용했지만 이 서버는 `POST /api/orders`에
인증을 요구합니다. 체크아웃 진입 전에 로그인을 붙여야 합니다.

## 아직 구현하지 않은 것

프론트가 쓰는 엔드포인트 중 다음은 남아 있습니다. 패턴은 위와 동일해서 기계적으로 추가할 수 있습니다.

- `support` 컨텍스트 — `/api/reviews`, `/api/qna`, 관리자 문의 답변
- `/api/search/suggestions`, `/api/search/trending`
- `/api/promotions` (타임세일 배너), `/api/addresses` (배송지 주소록)
- 회원가입, 위시리스트 서버 동기화

## 운영 전 반드시 할 것

- `JWT_SECRET` 환경변수 주입 (기본값은 로컬 전용)
- `hoonshop.seed.enabled=false`
- `MockPaymentGateway` → 실제 PG 어댑터 교체.
  **카드번호를 서버로 받지 마세요.** PG 결제창이 발급한 `paymentKey`만 받아 승인합니다.
- `CORS_ORIGINS`를 실제 배포 주소로 제한
