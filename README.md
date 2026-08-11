# hoonshop 🧵

비즈 액세서리 쇼핑몰 프론트엔드. 도메인 단위로 격리된 Redux 설계 위에, 수공예 브랜드에 맞춘
에디토리얼 UI를 얹었습니다.

```bash
npm install
npm run dev      # http://localhost:5173
```

백엔드 없이 바로 돕니다. MSW(Mock Service Worker)가 `/api/*` 요청을 가로채 실제 서버처럼
응답하므로, 상품 조회부터 결제 승인까지 전 과정을 브라우저에서 그대로 체험할 수 있습니다.

| 계정 | 이메일 | 비밀번호 | 들어가는 곳 |
|---|---|---|---|
| 고객 | `hoon@example.com` | `hoonshop` | 마이페이지 |
| 관리자 | `admin@hoonshop.com` | `hoonshop` | `/admin` 관리자 콘솔 |

**결제 거절 시나리오** — 카드번호 끝 4자리를 `0000`으로 입력

---

## 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| 빌드 | **Vite 8** | 초기 구동 0.3초. 쇼핑몰은 화면 수가 많아 HMR 속도가 곧 생산성입니다. |
| UI | **React 19** | 제공된 설계안이 React 기준이며, 생태계가 가장 두껍습니다. |
| 상태 | **Redux Toolkit + RTK Query** | 서버 상태(캐시·무효화)와 클라이언트 상태(장바구니·주문 단계)를 한 스토어에서 분리 관리합니다. |
| 라우팅 | **React Router 7** | 필터·검색 조건을 URL에 두어 공유·뒤로가기가 자연스럽게 동작합니다. |
| 스타일 | **Tailwind CSS 4** | CSS-first 설정(`@theme`)으로 디자인 토큰과 유틸리티가 한 파일에서 관리됩니다. |
| 목 API | **MSW 2** | 백엔드 연동 전까지 네트워크 레벨에서 응답을 흉내 냅니다. 실서버 붙일 때 `src/mocks/`만 지우면 끝. |
| 아이콘 | **lucide-react** | 이모지 대신 일관된 SVG 아이콘. |

> **JavaScript로 유지한 이유** — 제공된 설계안이 `.js/.jsx` 기준이라 그대로 따랐습니다.
> 상품 옵션·주문 payload처럼 구조가 깊어지는 지점이 있어, 규모가 커지면 TypeScript 전환을
> 권합니다. 경로 별칭(`@/`)과 도메인 분리가 이미 되어 있어 파일 단위로 점진 전환이 가능합니다.

---

## 디자인 시스템

`ui-ux-pro-max` 스킬의 스타일·팔레트·타이포 데이터베이스를 조회해 방향을 잡고,
비즈 공예 브랜드에 맞게 조정했습니다.

**스타일** — Editorial Boutique (과장된 미니멀리즘 × 공예의 온기)
큰 여백, 큰 세리프 제목, 각진 상품 프레임. 장식은 줄이고 상품이 화면을 차지하게 했습니다.

**컬러** — 웜 아이보리 캔버스 + 로즈우드 플럼 + 샴페인 골드

| 토큰 | 값 | 용도 | 대비 |
|---|---|---|---|
| `--color-canvas` | `#FBF8F4` | 배경 | — |
| `--color-ink` | `#241E1B` | 본문 | 15.4:1 |
| `--color-ink-soft` | `#6F6259` | 보조 텍스트 | 5.1:1 |
| `--color-primary` | `#7A3B52` | CTA·강조 | 8.1:1 (on white) |
| `--color-accent` | `#B08D57` | 장식·보더 전용 | 3.1:1 |
| `--color-accent-ink` | `#7D6134` | 골드 계열 **텍스트** | 5.0:1 |
| `--color-sale` | `#B3261E` | 할인가 | 5.9:1 |

액센트 골드는 텍스트로 쓰면 4.5:1을 넘지 못해, 글자용 변형(`accent-ink`)을 따로 두었습니다.

**타이포** — Cormorant(영문 디스플레이) / Pretendard(본문·한글) / Montserrat(라벨)
Cormorant에는 한글 글리프가 없어 국문 제목은 Pretendard로 렌더됩니다. 가짜 이탤릭은
자모가 뭉개지므로 쓰지 않고 색으로만 강조합니다.

**모션** — 150–300ms, `cubic-bezier(0.22, 1, 0.36, 1)`.
`transform`/`opacity`만 애니메이트하고, `prefers-reduced-motion`을 전역에서 존중합니다.

---

## 상품 이미지에 대해

스톡 사진 대신 **`BeadArt` 컴포넌트가 상품마다 비즈 가닥을 SVG로 직접 그립니다.**
상품 id를 시드로 쓰는 결정론적 난수라 같은 상품은 언제나 같은 모양이고, 카테고리에 따라
목걸이는 곡선, 팔찌는 원, 귀걸이는 두 가닥으로 배치됩니다. 컬러 옵션을 바꾸면 배경·하이라이트·
줄까지 함께 바뀝니다.

외부 이미지 요청이 0이라 LCP가 빠르고 이미지 깨짐이 없습니다. 실제 촬영본이 준비되면
`BeadArt`를 `<img>`로 바꾸되, 비율 고정 래퍼(`media-frame`)는 남겨 두어 CLS를 막으세요.

---

## 디렉토리 구조

```
src/
├─ app/          store · rootReducer · persistMiddleware · App
├─ api/          baseQuery (토큰 주입 + 401 자동 갱신)
├─ components/   common(Button·Sheet·Field·Toast·BeadArt…)
│                layout(StoreLayout·AdminLayout·Header·Footer·BottomNav)
├─ features/     도메인별 slice · api · 전용 UI
│  ├─ auth/      authSlice · authApi · LoginView
│  ├─ products/  productApi · ProductCard · ProductGrid · FilterBar
│  ├─ search/    searchSlice · searchApi · SearchOverlay
│  ├─ cart/      cartSlice · wishlistSlice · CartDrawer · FreeShippingMeter
│  ├─ order/     orderSlice · orderApi · OrderForm
│  ├─ payment/   paymentSlice · PaymentModule
│  ├─ cs/        reviewApi · ReviewSection · ReviewForm · QnaList
│  └─ marketing/ promoSlice · promoApi · couponRules · EventBanner · CouponSheet
├─ pages/        라우트 단위 화면
├─ hooks/        useDebounce · useBodyScrollLock · useCountdown
├─ mocks/        db · handlers · browser  ← 실서버 연동 시 삭제
├─ styles/       global.css (디자인 토큰 전부)
└─ utils/        format · validate · storage
```

---

## 도메인별 구현 노트

**1. 인증** — `baseQuery`가 모든 요청 헤더에 토큰을 주입합니다. 401이 오면 refresh 토큰으로
재발급 후 원 요청을 1회 재시도하고, 동시에 401이 여러 건 터져도 refresh는 한 번만 나가도록
프로미스를 잠급니다.

**2. 상품** — RTK Query의 `serializeQueryArgs`/`merge`로 무한 스크롤을 구현했습니다.
page를 제외한 조건이 같으면 같은 캐시에 이어붙고, 필터가 바뀌면 새 캐시가 생성됩니다.
스크롤 이벤트 대신 `IntersectionObserver`를 씁니다.

**3. 검색** — 250ms 디바운스 후 자동완성 요청. 최근 검색어는 로컬에만 남습니다.

**4. 장바구니** — 라인의 정체성은 `상품ID + 옵션`입니다. 같은 목걸이라도 민트/42cm와
라일락/38cm는 별개의 줄입니다. 옵션을 바꿔 기존 줄과 겹치면 수량을 합칩니다.
금액 계산은 `selectCartSummary` 하나에서만 하고 장바구니·주문서·결제가 이를 공유합니다.

**5. 주문** — 저장된 배송지 선택 / 신규 입력을 한 화면에서 다룹니다. 검증은 blur 시점에
실행하고, 고치는 즉시 에러를 지웁니다.

**6. 결제** — 승인 직전 `POST /orders/validate`로 재고를 다시 확인합니다. 그 사이 품절되었거나
수량이 부족하면 어떤 상품이 왜 문제인지 짚어 보여줍니다.
> ⚠️ 데모의 카드 입력 폼은 흐름 시연용입니다. 실서비스에서는 카드번호가 우리 서버에 닿으면
> 안 되며, PG SDK가 발급한 `paymentKey`만 넘기고 **금액 검증은 서버가** 해야 합니다.

**7. 리뷰·Q&A** — 다중 이미지 업로드는 파일별 진행률을 따로 표시합니다. 전체 스피너 하나로는
어디까지 됐는지 알 수 없습니다.

**8. 프로모션** — `couponRules.js`가 순수 함수로 분리되어 있습니다. 단독 사용 쿠폰 1장 vs
중복 가능 쿠폰 전부를 비교해 최대 할인 조합을 찾고, 못 쓰는 쿠폰도 숨기지 않고 **왜** 못 쓰는지
함께 보여줍니다.

---

## 관리자 콘솔 (`/admin`)

`admin@hoonshop.com`으로 로그인하면 들어갑니다. 고객 화면과 셸이 완전히 분리되어 있어
헤더·푸터·하단 내비 대신 사이드바를 쓰고, 라우트도 `StoreLayout` / `AdminLayout`으로 나뉩니다.

| 화면 | 하는 일 |
|---|---|
| 대시보드 | 오늘 매출(히어로 지표), 처리 대기 3종, 최근 14일 매출 차트, 주문 상태 분포, 판매 상위, 재고 경고 |
| 주문 관리 | 상태별 필터·검색, 표(데스크톱)/카드(모바일), 다음 단계로 상태 전환 |
| 상품·재고 | 재고 적은 순 정렬, 인라인 재고 수정 (Enter 저장 / Esc 취소) |
| 문의 | 미답변 필터, 자주 쓰는 문구로 답변 작성 |

**밀도를 다르게 잡았습니다.** 쇼핑 화면은 여백으로 상품을 돋보이게 하지만, 운영 화면은 한 화면에
얼마나 담기느냐가 곧 업무 속도입니다. 대신 색·서체 토큰은 공유해서 같은 브랜드로 읽히게 했습니다.

**권한 가드** — `RequireAdmin`이 비로그인(로그인 페이지로 유도)과 권한 없음(이유를 설명하고 정지)을
다르게 처리합니다. 둘 다 로그인 페이지로 보내면 고객 계정으로는 무한 루프에 빠집니다.
> ⚠️ 프론트 가드는 UX 장치일 뿐입니다. 실제 차단은 서버가 토큰의 role을 검사해서 해야 합니다.

**낙관적 업데이트** — 주문 상태 변경은 응답을 기다리지 않고 먼저 반영합니다. 목록에서 연속으로
누르는 작업이라 매번 멈추면 같은 버튼을 두 번 누르게 됩니다. 실패하면 되돌립니다.

**재고 수정 후 행이 튀지 않습니다.** 목록은 재고 오름차순인데, 수정 직후 재조회하면 방금 고친 행이
저 아래로 사라집니다. 그래서 목록을 무효화하는 대신 캐시를 제자리에서 고칩니다.

### 차트 규칙

시각화는 다음을 지켰습니다.

- **단일 계열은 한 색.** 매출 막대를 값이 클수록 진하게 칠하면 막대 길이가 이미 말하는 정보를
  색으로 중복 인코딩하게 됩니다.
- **주문 상태는 순서가 있는 단계**이므로 categorical이 아니라 **ordinal 램프**(브랜드 플럼 4단계)를
  씁니다. 밝기 단조 증가 + 가장 밝은 단계도 흰 배경 대비 2:1 이상 — 검증 스크립트 통과.
  '취소'는 진행 단계가 아니라서 퍼널 막대에서 빼고 따로 표기합니다.
- **눈금 간격을 먼저 반올림**하고 상한을 정합니다. 상한만 반올림하면 `13만 / 25만 / 38만` 같은
  눈금이 나옵니다.
- **값 라벨은 최고치 하루에만.** 모든 막대에 숫자를 붙이면 아무도 읽지 않습니다.
- **툴팁이 값을 읽는 유일한 통로가 되지 않도록** 표 보기를 함께 제공하고, 키보드 포커스도 hover와
  같은 정보를 보여줍니다.
- 격자선은 실선 hairline (점선은 '임계값'으로 오해됩니다), 막대 두께 24px 상한, 누적 막대 경계는
  테두리가 아니라 2px 배경색 간격.
- 상태 배지는 색만으로 구분하지 않고 **아이콘 + 한글 라벨**이 항상 붙습니다.

---

## 접근성 체크리스트

- [x] 본문 대비 4.5:1↑ / UI 요소 3:1↑ — 팔레트 단계에서 검증
- [x] 포커스 링 유지 (`:focus-visible`), 오버레이 포커스 트랩 + Esc 닫기 + 원위치 복원
- [x] 터치 타깃 44×44px 이상, 인접 타깃 8px 이상 간격
- [x] 모든 입력에 보이는 라벨, 에러는 필드 바로 아래 + `aria-describedby` 연결
- [x] 아이콘 전용 버튼 전부 `aria-label`
- [x] 색만으로 정보 전달하지 않음 (별점·재고·진행률에 숫자/문장 병기)
- [x] `prefers-reduced-motion` 존중
- [x] 본문 바로가기 링크, 라우트 변경 시 스크롤 처리(뒤로가기는 위치 유지)
- [x] 375 / 768 / 1024 / 1440px 확인, 가로 스크롤 없음
- [x] 이미지 비율 고정으로 CLS 억제, 로딩은 스켈레톤

---

## 실서버 연동할 때

1. `src/main.jsx`의 `startMockServer()` 호출 제거
2. `src/mocks/` 삭제, `public/mockServiceWorker.js` 삭제
3. `src/api/baseQuery.js`의 `baseUrl`을 실제 API 주소로 변경
4. 응답 스키마가 다르면 각 `*Api.js`의 `transformResponse`로 흡수

`src/mocks/handlers.js`가 곧 필요한 엔드포인트 명세입니다. 백엔드에 그대로 넘기면 됩니다.
