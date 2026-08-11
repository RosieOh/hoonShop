# hoonshop API

비즈 액세서리 쇼핑몰 백엔드. **Spring Boot 3 + Java 17**, DDD 전술 패턴으로 구성했습니다.

```bash
docker compose up -d      # PostgreSQL (호스트 5433 포트)
./gradlew bootRun         # http://localhost:8080
./gradlew test            # 도메인 단위 테스트 + 아키텍처 검사
```

- API 문서 — http://localhost:8080/swagger-ui.html
- 데모 계정 — `hoon@example.com` / `admin@hoonshop.com`, 비밀번호 공통 `hoonshop`

**결제 시나리오 재현** — `paymentKey` 접두사로 고릅니다 (목 PG).

| paymentKey | 결과 |
|---|---|
| `DECLINE_...` | 카드 승인 거절 (확정된 실패) |
| `TIMEOUT_...` | 응답 없음 → UNKNOWN → 대사 대상 |
| `VBANK_...` | 가상계좌 발급 (입금 웹훅 대기) |
| `WRONG_AMOUNT_...` | 요청과 다른 금액 승인 (교차검증 테스트) |
| 그 외 | 정상 승인 |

**소셜 로그인 재현** — 목 프로바이더는 인가 코드를 그대로 해석합니다.
`code=someone@example.com` → 그 이메일로, `code=noemail-길동` → 이메일 미제공 상황.

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

### 5. 결제 — 가장 공들인 부분

**5-1. 카드번호가 서버에 닿지 않습니다.**
`PaymentGateway` 포트에 카드 관련 필드가 아예 없습니다. PG 결제창(SDK)이 발급한
`paymentKey`만 받습니다. 카드번호가 한 번이라도 서버를 거치면 그 순간부터 로그 마스킹,
저장 암호화, 정기 감사가 전부 우리 책임(PCI-DSS 범위)이 됩니다. 애초에 받지 않는 게
유일하게 확실한 방법입니다.

**5-2. PG 호출이 DB 트랜잭션 밖에 있습니다.**
승인 로직을 통째로 `@Transactional`로 감싸면, PG 응답을 기다리는 수 초 동안 DB 커넥션과
재고 락을 붙잡습니다. PG가 느려지는 순간 커넥션 풀이 마르고 **결제와 무관한 상품 조회까지
전부 멈춥니다.** 결제 장애가 사이트 전체 장애로 번지는 가장 흔한 경로입니다. 그래서 셋으로 쪼갰습니다.

```
[Tx1 커밋] 멱등 확인 + 승인 시도 기록   ← PG를 부르기 전에 커밋
     ↓
[Tx 없음]  PG 승인 호출 (타임아웃 있음)
     ↓
[Tx2 커밋] 결과 반영 → 주문 결제완료 → 재고 차감
```

`ConfirmPaymentService`에 `@Transactional`이 없는 것은 실수가 아닙니다.
트랜잭션 경계는 `PaymentTransactionService`가 담당합니다.

**5-3. "결과를 모른다"를 별도 상태로 둡니다.**
대부분의 구현이 성공/실패만 다루는데, 실제 사고는 **세 번째 경우**에서 납니다.
승인 요청 후 타임아웃이 나면 승인됐는지 알 수 없습니다.

- 실패로 처리 → 고객 돈은 빠져나갔는데 주문이 없음
- 성공으로 처리 → 받지도 않은 돈으로 상품 발송

그래서 `PaymentStatus.UNKNOWN`으로 기록하고, `PaymentReconciliationService`가 5분마다
PG에 다시 물어 확정합니다. PG를 부르기 직전에 서버가 죽어 `REQUESTED`로 멈춘 건도 대상입니다.

**5-4. 승인 금액을 교차검증합니다.**
PG 응답 금액이 우리가 요청한 금액과 다르면 승인을 인정하지 않고 **즉시 보상 취소**합니다.
보상 취소마저 실패하면 고객 돈을 들고 있는 상태이므로 반드시 에러 로그로 남깁니다.

**5-5. 멱등키.**
`payment.idempotency_key` UNIQUE 제약. 애플리케이션이 확인하고, 뚫려도 DB가 막습니다.

**5-6. 결제 원장(`payment_ledger`).**
상태가 바뀔 때마다 append-only로 쌓습니다. `status` 컬럼만으로는 "지금 어떤 상태인가"만
알 수 있고 "어떻게 여기까지 왔는가"는 알 수 없는데, 결제 분쟁은 대부분 후자를 묻습니다.

**5-7. 가상계좌 = 승인 ≠ 입금.**
발급 시점에는 돈이 들어오지 않습니다. `WAITING_FOR_DEPOSIT`으로 두고, 입금 웹훅을 받아야
주문이 결제 완료가 되고 그때 재고가 빠집니다. 웹훅은 HMAC 서명을 검증하고(없으면 누구나
"입금됐다"고 우길 수 있음), 재전송에 대비해 멱등하며, 우리 쪽 실패 시에도 200을 돌려줍니다
(4xx/5xx를 주면 PG가 무한 재전송합니다).

**5-8. 취소는 환불 먼저, 주문 취소는 그 다음.**
반대로 하면 주문은 취소됐는데 환불이 실패한 상태가 남아 고객이 상품도 돈도 잃습니다.
그리고 **취소의 주도권은 주문에 있습니다** — 결제 취소가 주문을 취소시키고 주문 취소가
결제를 취소시키면 순환이 생깁니다. 관리자 상태변경 API(`PATCH`)로는 취소할 수 없고
전용 취소 API를 쓰도록 막아두었습니다(환불 누락 방지).

### 5-9. 소셜 로그인

카카오·네이버·구글. 인가 코드를 **서버가** 토큰으로 교환합니다 — 프론트가 액세스 토큰을
직접 받아 넘기면 그 토큰이 우리 앱을 위해 발급된 것인지 검증할 수 없습니다.

- **식별자는 이메일이 아니라 `providerUserId`** — 카카오는 이메일을 안 줄 수 있고,
  사용자가 소셜 계정 이메일을 바꿀 수도 있습니다. 이메일로 식별하면 다른 사람으로
  인식되거나 더 나쁘게는 남의 계정에 붙습니다.
- **검증된 이메일만 인정** — 카카오 `is_email_verified`, 구글 `email_verified`가 false면
  없는 것으로 취급합니다. 미검증 이메일로 기존 계정에 자동 연결하면 계정 탈취 경로가 됩니다.
- **이메일이 없으면** `<provider>_<id>@social.hoonshop.invalid`로 내부 식별자를 만듭니다.
  `.invalid`는 RFC 6761이 "절대 실재하지 않음"으로 예약한 도메인이라 오발송 사고가 없습니다.
- **목이 운영에 새지 않도록** — 같은 프로바이더에 실제 어댑터와 목이 함께 있으면 실제 쪽을
  씁니다. 목이 선택되면 인가 코드 검증 없이 아무나 로그인됩니다.

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

### 9. 테이블 이름은 전부 `tbl_` 접두사

`tbl_user` `tbl_product` `tbl_product_palette` `tbl_product_material` `tbl_product_size`
`tbl_product_color_option` `tbl_product_badge` `tbl_inventory` `tbl_coupon` `tbl_order`
`tbl_order_line` `tbl_order_coupon` `tbl_payment` `tbl_payment_ledger` `tbl_social_account`

접두사 덕분에 **예약어 회피용 이름이 필요 없어졌습니다.** 원래 `user`와 `order`가 SQL
예약어라 `app_user` / `orders`로 우회했는데, 이제 `tbl_user` / `tbl_order`처럼 도메인 용어를
그대로 씁니다. 시퀀스(`order_number_seq`)는 테이블이 아니라 그대로 두었습니다.

엔티티의 `@Table` / `@CollectionTable`만 바뀌었고 JPQL은 엔티티명을 쓰므로 쿼리는
손대지 않았습니다. `ddl-auto=validate`라 이름이 하나라도 어긋나면 애플리케이션이
기동하지 않습니다 — 기동에 성공한 것 자체가 15개 테이블 전부 일치한다는 검증입니다.

> V1·V2 마이그레이션을 제자리에서 고쳤습니다(운영 데이터가 없는 단계라 이름 변경
> 마이그레이션을 따로 쌓는 것보다 깨끗합니다). 이미 예전 스키마로 DB를 만든 적이 있다면
> 체크섬이 어긋나므로 스키마를 드롭하고 다시 올리세요:
> `docker compose down -v && docker compose up -d`

### 10. 원화는 `long`, `Money` 값 객체로

`double`은 부동소수점 오차로 금액에 쓸 수 없고, `BigDecimal`은 원화에 없는 소수부 때문에
스케일·반올림 규칙을 매번 신경 써야 합니다. `Money`는 음수를 허용하지 않고, 뺄셈이 음수가
되면 0으로 자르지 않고 **예외를 던집니다** — 조용히 0이 되면 잘못된 청구를 못 찾습니다.

---

## 검증 상태

```
./gradlew test          71개 통과 (도메인 단위 + 아키텍처 규칙)
API 통합 시나리오        75개 통과 (실제 PostgreSQL 대상, 2개 스위트)
```

**커머스 기본**: 필터·정렬·검색, 로그인/리프레시(토큰 타입 검증), 쿠폰 사용 불가 사유,
재고 사전 확인, 금액 조작 차단, 옵션 위조 차단, 상태 전이 규칙, 권한(403).

**결제**: 카드번호 경로 부재, 승인 금액 교차검증 후 거부, PG 거절(402), 타임아웃 →
UNKNOWN(실패로 뭉뚱그리지 않음), 확인 중 재시도 차단, 가상계좌 발급 시점에는 재고 미차감 →
입금 웹훅 후 차감, 웹훅 재전송 멱등, 취소 → 환불 → 재고 복원, PATCH로는 취소 불가.

**소셜 로그인**: 신규 가입, 재로그인 시 중복 가입 없음, 기존 회원 이메일과 일치 시 연결,
이메일 미제공 가입, `.invalid` 내부 이메일, 소셜 토큰으로 일반 API 사용.

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

```bash
JWT_SECRET=<32바이트 이상>          # 기본값은 로컬 전용
PAYMENT_PROVIDER=toss
TOSS_SECRET_KEY=<토스 시크릿키>
TOSS_WEBHOOK_SECRET=<웹훅 시크릿>   # 없으면 서명 검증이 항상 실패합니다
OAUTH_MOCK=false                    # 목이 뜨면 아무나 로그인됩니다
KAKAO_CLIENT_ID=... NAVER_CLIENT_ID=... GOOGLE_CLIENT_ID=...
CORS_ORIGINS=https://실제도메인
```

- `hoonshop.seed.enabled=false`
- `PaymentNeedsReconciliation` 이벤트를 **알림(슬랙·이메일)에 연결하세요.**
  지금은 로그만 남습니다. 고객 돈이 걸린 사건이라 사람이 즉시 봐야 합니다.
- 대사 배치가 여러 인스턴스에서 동시에 돌지 않도록 스케줄러 잠금(ShedLock 등)을 붙이세요.
- `application.yml`에 OAuth `client-id`를 **빈 문자열로 두지 마세요.**
  `@ConditionalOnProperty`는 "빈 값"과 "미설정"을 구분하지 않아, 빈 문자열이어도 실제
  어댑터가 등록되고 목 대신 그쪽으로 요청이 나갑니다(실제로 겪은 버그입니다).
