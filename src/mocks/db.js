/**
 * 목 데이터베이스 (백엔드 붙기 전까지 MSW가 이 데이터를 서빙합니다)
 * 실제 API로 교체할 때는 이 파일과 handlers.js만 지우면 됩니다.
 */

export const CATEGORIES = [
  { id: 'all', label: '전체', en: 'All' },
  { id: 'necklace', label: '목걸이', en: 'Necklace' },
  { id: 'bracelet', label: '팔찌', en: 'Bracelet' },
  { id: 'earring', label: '귀걸이', en: 'Earring' },
  { id: 'ring', label: '반지', en: 'Ring' },
  { id: 'anklet', label: '발찌', en: 'Anklet' },
  { id: 'strap', label: '키링·스트랩', en: 'Strap' },
];

export const SORT_OPTIONS = [
  { id: 'recommend', label: '추천순' },
  { id: 'new', label: '신상품순' },
  { id: 'popular', label: '인기순' },
  { id: 'price_asc', label: '낮은 가격순' },
  { id: 'price_desc', label: '높은 가격순' },
  { id: 'review', label: '리뷰 많은순' },
];

/** 비즈 색 조합 — 상품 아트워크와 옵션 스와치에 함께 쓰입니다. */
const COLORWAYS = {
  butter: ['#F6D89B', '#FFF2D6', '#E8B96A', '#FBE7BC'],
  peach: ['#F5B5A3', '#FFE1D6', '#E08C74', '#FCD3C6'],
  mint: ['#A8D8C8', '#DFF3EC', '#6FBBA4', '#C8E9DE'],
  lilac: ['#C4B0DE', '#EBE2F6', '#9C82C4', '#DCCFEE'],
  ocean: ['#9CC3E0', '#DCEBF7', '#6296C0', '#C3DCEE'],
  cherry: ['#E58BA0', '#FBD7DF', '#C75F79', '#F2BCC8'],
  cocoa: ['#C6A488', '#EEDFD1', '#9C7A5B', '#E0CBB6'],
  pearl: ['#F1EAE2', '#FFFFFF', '#D9CCBE', '#FAF5EF'],
  olive: ['#B9C79A', '#E5EBD5', '#8FA070', '#D3DCBE'],
  ink: ['#6B6E8C', '#C9CBDD', '#474A66', '#A6A9C2'],
};

export const COLOR_SWATCHES = [
  { id: 'butter', label: '버터크림', hex: '#F6D89B' },
  { id: 'peach', label: '피치', hex: '#F5B5A3' },
  { id: 'mint', label: '민트', hex: '#A8D8C8' },
  { id: 'lilac', label: '라일락', hex: '#C4B0DE' },
  { id: 'ocean', label: '오션', hex: '#9CC3E0' },
  { id: 'cherry', label: '체리', hex: '#E58BA0' },
  { id: 'cocoa', label: '코코아', hex: '#C6A488' },
  { id: 'pearl', label: '펄화이트', hex: '#F1EAE2' },
  { id: 'olive', label: '올리브', hex: '#B9C79A' },
  { id: 'ink', label: '잉크블루', hex: '#6B6E8C' },
];

const raw = [
  // --- 목걸이 ---
  ['오후 세 시의 버터', 'necklace', 'butter', 32000, 0.15, 4.8, 214, ['best'], '체코 파이어폴리시 비즈에 14K 골드필드 클래스프. 햇빛 각도에 따라 결이 달라집니다.'],
  ['진주 한 알만', 'necklace', 'pearl', 46000, 0, 4.9, 331, ['best'], '담수 바로크 진주 한 알을 실버 체인 정중앙에 물렸습니다. 어떤 옷에도 방해되지 않는 크기.'],
  ['라일락 스몰토크', 'necklace', 'lilac', 29000, 0, 4.6, 87, ['new'], '4mm 라일락 유리 비즈를 촘촘히. 목선을 짧게 감싸는 초커 길이입니다.'],
  ['체리 콤포트', 'necklace', 'cherry', 35000, 0.2, 4.7, 152, [], '체리색 라운드 비즈와 골드 시드 비즈를 번갈아 꿰어 리듬감을 만들었습니다.'],
  ['미드나잇 링크', 'necklace', 'ink', 52000, 0, 4.8, 64, ['limited'], '잉크블루 큐브 비즈 한정 수량. 재입고 계획이 없는 컬러입니다.'],

  // --- 팔찌 ---
  ['민트 소다 팔찌', 'bracelet', 'mint', 19000, 0, 4.7, 402, ['best'], '탄성 실 사용으로 잠금장치 없이 착용. 여름에 가장 많이 나가는 스테디셀러.'],
  ['피치 마카롱', 'bracelet', 'peach', 22000, 0.1, 4.5, 188, [], '무광 아크릴 비즈라 부딪혀도 소리가 나지 않습니다. 사무실용으로 추천.'],
  ['코코아 위빙', 'bracelet', 'cocoa', 27000, 0, 4.6, 96, ['new'], '두 줄을 엮어 짠 위빙 팔찌. 손목이 얇아도 흘러내리지 않습니다.'],
  ['오션 드립', 'bracelet', 'ocean', 24000, 0, 4.4, 73, [], '투명 비즈 사이로 빛이 통과하며 물방울처럼 보입니다.'],
  ['데이지 체인', 'bracelet', 'olive', 21000, 0.15, 4.8, 245, ['best'], '시드 비즈로 한 송이씩 데이지를 엮었습니다. 한 줄 완성에 40분.'],

  // --- 귀걸이 ---
  ['버터 드롭 이어링', 'earring', 'butter', 26000, 0, 4.9, 176, ['best'], '침 부분은 순은. 귀 예민한 분들 후기가 특히 좋습니다.'],
  ['펄 미니 후프', 'earring', 'pearl', 31000, 0, 4.8, 289, [], '2cm 후프에 미니 진주 5알. 가벼워서 종일 착용해도 늘어지지 않습니다.'],
  ['라일락 캔디', 'earring', 'lilac', 23000, 0.2, 4.5, 61, ['new'], '사탕처럼 둥근 비즈 두 알. 캐주얼한 데일리 룩에.'],
  ['체리 롱 스트랜드', 'earring', 'cherry', 34000, 0, 4.6, 44, ['limited'], '6cm 롱 드롭. 얼굴선을 길어 보이게 합니다.'],

  // --- 반지 ---
  ['시드 비즈 링 세트', 'ring', 'mint', 15000, 0, 4.7, 358, ['best'], '3개 한 세트. 겹쳐 끼거나 따로 끼거나.'],
  ['버터 큐브 링', 'ring', 'butter', 13000, 0.1, 4.4, 129, [], '사각 비즈 하나만 올린 미니멀 링.'],
  ['오션 트위스트', 'ring', 'ocean', 17000, 0, 4.5, 82, ['new'], '두 겹으로 꼬아 만든 반지. 프리 사이즈.'],

  // --- 발찌 ---
  ['모래알 발찌', 'anklet', 'pearl', 18000, 0, 4.6, 141, [], '발목에서 잘게 부서지는 빛. 방수 코팅 처리.'],
  ['올리브 리프 발찌', 'anklet', 'olive', 20000, 0.15, 4.5, 67, ['new'], '잎사귀 참을 하나 달았습니다. 길이 조절 3단계.'],

  // --- 키링·스트랩 ---
  ['비즈 폰 스트랩', 'strap', 'cherry', 25000, 0, 4.8, 512, ['best'], '손목 통과 사이즈. 무게 12g으로 폰 흔들림이 적습니다.'],
  ['버터 키링', 'strap', 'butter', 16000, 0, 4.6, 203, [], '가방에 달면 끝. 링 부분은 무니켈 도금.'],
  ['민트 백 참', 'strap', 'mint', 19000, 0.25, 4.4, 88, [], '시즌 오프 할인 중. 재고 소진 시 종료됩니다.'],
  ['잉크 카드홀더 스트랩', 'strap', 'ink', 28000, 0, 4.7, 55, ['new'], '사원증·카드홀더용 넥스트랩. 안전 버클 포함.'],
  ['코코아 에어팟 참', 'strap', 'cocoa', 17000, 0, 4.5, 119, [], '에어팟 케이스 고리에 딱 맞는 사이즈.'],
];

const SIZE_BY_CATEGORY = {
  necklace: ['38cm (초커)', '42cm (기본)', '45cm (레이어드)'],
  bracelet: ['15cm (S)', '16.5cm (M)', '18cm (L)'],
  anklet: ['22cm', '24cm', '26cm'],
  ring: ['9호', '11호', '13호', '15호'],
  earring: [],
  strap: [],
};

const DAY = 86_400_000;

/**
 * 시드 데이터의 기준 시각.
 *
 * 고정 날짜를 박아두면 배포 후 시간이 흐를수록 데이터가 과거로 밀려나고,
 * 대시보드의 "최근 14일" 창에서 전부 빠져나가 빈 차트가 됩니다.
 * 앱이 로드되는 시점을 기준으로 잡아 언제 열어봐도 살아있는 데모가 되게 합니다.
 */
const EPOCH = Date.now();

export const PRODUCTS = raw.map((row, i) => {
  const [name, category, colorway, price, discountRate, rating, reviewCount, badges, description] = row;
  const salePrice = discountRate ? Math.round((price * (1 - discountRate)) / 100) * 100 : null;
  const sizes = SIZE_BY_CATEGORY[category] ?? [];

  // 컬러 옵션: 기본 컬러웨이 + 인접 2종
  const swatchIdx = COLOR_SWATCHES.findIndex((c) => c.id === colorway);
  const colorOptions = [0, 3, 6].map(
    (offset) => COLOR_SWATCHES[(swatchIdx + offset) % COLOR_SWATCHES.length],
  );

  return {
    id: `P${String(i + 1).padStart(4, '0')}`,
    name,
    category,
    colorway,
    palette: COLORWAYS[colorway],
    price,
    salePrice,
    discountRate: discountRate ? Math.round(discountRate * 100) : 0,
    rating,
    reviewCount,
    badges,
    description,
    materials:
      category === 'earring'
        ? ['체코 유리 비즈', '925 실버 침', '골드필드 마감']
        : ['체코 유리 비즈', '스테인리스 와이어', '무니켈 도금 부자재'],
    sizes,
    colorOptions,
    // 재고: 결제 직전 재고 확인 로직을 시연하기 위해 일부러 희소한 값 포함
    stock: [3, 48, 120, 7, 0, 64, 25][i % 7],
    createdAt: new Date(EPOCH - i * 3 * DAY).toISOString(),
    soldCount: 1200 - i * 37,
  };
});

export const COUPONS = [
  {
    id: 'CPN-WELCOME',
    name: '신규 가입 15% 쿠폰',
    type: 'percent',
    value: 15,
    minAmount: 20000,
    maxDiscount: 8000,
    expiresAt: '2026-12-31T23:59:59Z',
    stackable: false,
    scope: 'all',
  },
  {
    id: 'CPN-SUMMER',
    name: '여름맞이 5,000원 할인',
    type: 'amount',
    value: 5000,
    minAmount: 40000,
    maxDiscount: 5000,
    expiresAt: '2026-09-30T23:59:59Z',
    stackable: true,
    scope: 'all',
  },
  {
    id: 'CPN-SHIP',
    name: '배송비 무료 쿠폰',
    type: 'shipping',
    value: 3000,
    minAmount: 0,
    maxDiscount: 3000,
    expiresAt: '2026-08-31T23:59:59Z',
    stackable: true,
    scope: 'shipping',
  },
  {
    id: 'CPN-EXPIRED',
    name: '봄 시즌 10% 쿠폰',
    type: 'percent',
    value: 10,
    minAmount: 10000,
    maxDiscount: 5000,
    expiresAt: '2026-05-31T23:59:59Z',
    stackable: false,
    scope: 'all',
  },
];

export const PROMOTIONS = [
  {
    id: 'EV-TIMESALE',
    kind: 'timesale',
    title: '오늘의 타임세일',
    subtitle: '자정까지 전 상품 추가 10%',
    endsAt: null, // 런타임에 '오늘 자정'으로 계산
    accent: '#B3261E',
  },
  {
    id: 'EV-HANDMADE',
    kind: 'banner',
    title: '한 알씩, 손으로',
    subtitle: '모든 제품은 주문 후 제작됩니다',
    accent: '#7A3B52',
  },
];

const REVIEW_SEEDS = [
  ['지현', 5, '사진보다 실물이 훨씬 예뻐요. 비즈 알이 균일해서 마감이 깔끔합니다.', 2],
  ['soo_0', 4, '생각보다 조금 짧았는데 레이어드하니까 오히려 좋네요. 재구매 의사 있어요.', 0],
  ['민트초코', 5, '선물용으로 샀는데 포장이 정말 정성스러웠어요. 손편지까지 들어있어서 감동.', 3],
  ['ha__', 4, '가볍고 착용감 좋아요. 다만 배송이 3일 정도 걸렸습니다.', 1],
  ['연우', 5, '금속 알레르기 있는데 하루 종일 껴도 아무렇지 않았어요.', 0],
];

export const REVIEWS = PRODUCTS.flatMap((p, pi) =>
  REVIEW_SEEDS.slice(0, (pi % 4) + 2).map((seed, ri) => {
    const [author, rating, content, photoCount] = seed;
    return {
      id: `${p.id}-R${ri + 1}`,
      productId: p.id,
      author,
      rating,
      content,
      photoCount,
      option: p.sizes.length ? `${p.colorOptions[0].label} / ${p.sizes[1]}` : p.colorOptions[0].label,
      createdAt: new Date(EPOCH - (pi * 5 + ri * 2) * DAY).toISOString(),
      helpful: (pi * 7 + ri * 13) % 40,
    };
  }),
);

export const QNA = PRODUCTS.slice(0, 8).map((p, i) => ({
  id: `${p.id}-Q1`,
  productId: p.id,
  author: ['김*연', '이*훈', '박*지', '최*아'][i % 4],
  secret: i % 3 === 0,
  question: [
    '사이즈 교환도 가능한가요?',
    '샤워할 때 착용해도 변색되지 않나요?',
    '선물 포장 요청할 수 있을까요?',
    '재입고 알림은 어디서 신청하나요?',
  ][i % 4],
  answer:
    i % 5 === 4
      ? null
      : '안녕하세요, 훈샵입니다. 수령 후 7일 이내 미착용 상태라면 사이즈 교환 가능합니다. 문의 감사합니다 :)',
  createdAt: new Date(EPOCH - i * 4 * DAY).toISOString(),
}));

export const ADDRESSES = [
  {
    id: 'ADDR-1',
    label: '집',
    recipient: '김태훈',
    phone: '010-2345-6789',
    zipcode: '04524',
    address1: '서울특별시 중구 세종대로 110',
    address2: '3층 302호',
    isDefault: true,
  },
  {
    id: 'ADDR-2',
    label: '회사',
    recipient: '김태훈',
    phone: '010-2345-6789',
    zipcode: '06236',
    address1: '서울특별시 강남구 테헤란로 152',
    address2: '11층',
    isDefault: false,
  },
];

export const DEMO_USER = {
  id: 'U-0001',
  email: 'hoon@example.com',
  name: '김태훈',
  role: 'customer',
  grade: 'GOLD',
  point: 3200,
  joinedAt: '2025-11-02T00:00:00Z',
};

export const ADMIN_USER = {
  id: 'U-0000',
  email: 'admin@hoonshop.com',
  name: '김태훈',
  role: 'admin',
  grade: 'STAFF',
  point: 0,
  joinedAt: '2025-09-01T00:00:00Z',
};

/* ========================================================================== */
/*  관리자용 시드 데이터                                                        */
/* ========================================================================== */

/**
 * 주문 상태는 순서가 있는 단계입니다 (취소만 예외).
 * 이 순서가 곧 대시보드 ordinal 색 램프의 순서이기도 합니다.
 */
export const ORDER_STATUS = [
  { id: 'PAID', label: '결제 완료', next: 'MAKING' },
  { id: 'MAKING', label: '제작 중', next: 'SHIPPED' },
  { id: 'SHIPPED', label: '발송', next: 'DELIVERED' },
  { id: 'DELIVERED', label: '배송 완료', next: null },
  { id: 'CANCELLED', label: '취소', next: null },
];

const BUYERS = [
  ['이서연', 'seoyeon@example.com', '010-4412-8830', '서울특별시 마포구 양화로 45'],
  ['박도윤', 'doyun@example.com', '010-9921-2043', '경기도 성남시 분당구 판교역로 235'],
  ['최하은', 'haeun@example.com', '010-3388-7712', '부산광역시 해운대구 센텀중앙로 79'],
  ['정민준', 'minjun@example.com', '010-7745-1120', '인천광역시 연수구 컨벤시아대로 165'],
  ['강수아', 'sua@example.com', '010-2210-5567', '대전광역시 유성구 대학로 291'],
  ['윤지호', 'jiho@example.com', '010-6634-9081', '광주광역시 서구 상무중앙로 58'],
  ['임채원', 'chaewon@example.com', '010-5519-3376', '서울특별시 성동구 왕십리로 83'],
  ['오건우', 'gunwoo@example.com', '010-8802-4419', '경기도 고양시 일산동구 중앙로 1275'],
];

/** 시드 주문 22건. 최근 14일에 걸쳐 분포하며 상태가 섞여 있습니다. */
export const SEED_ORDERS = Array.from({ length: 22 }, (_, i) => {
  const daysAgo = Math.floor((i * 13) / 21); // 0~13일 전에 고르게 분포
  const buyer = BUYERS[i % BUYERS.length];
  const lineCount = (i % 3) + 1;

  const items = Array.from({ length: lineCount }, (_, k) => {
    const product = PRODUCTS[(i * 5 + k * 7) % PRODUCTS.length];
    const quantity = ((i + k) % 3) + 1;
    return {
      productId: product.id,
      name: product.name,
      options: {
        color: product.colorOptions[0].id,
        colorLabel: product.colorOptions[0].label,
        size: product.sizes[k % Math.max(1, product.sizes.length)] ?? null,
      },
      quantity,
      price: product.salePrice ?? product.price,
    };
  });

  const subtotal = items.reduce((sum, it) => sum + it.price * it.quantity, 0);
  const shippingFee = subtotal >= 50_000 ? 0 : 3_000;

  // 오래된 주문일수록 뒤 단계로. 3건은 취소 처리해 예외 흐름도 보이게 합니다.
  const status =
    i % 9 === 4
      ? 'CANCELLED'
      : daysAgo >= 10
        ? 'DELIVERED'
        : daysAgo >= 6
          ? 'SHIPPED'
          : daysAgo >= 2
            ? 'MAKING'
            : 'PAID';

  const createdAt = new Date(EPOCH - daysAgo * DAY - (i % 7) * 3_600_000).toISOString();

  return {
    id: `ORD-2026-${String(900 - i).padStart(5, '0')}`,
    customer: { name: buyer[0], email: buyer[1] },
    items,
    shippingAddress: {
      recipient: buyer[0],
      phone: buyer[2],
      zipcode: String(10000 + i * 137).slice(0, 5),
      address1: buyer[3],
      address2: `${(i % 15) + 1}0${(i % 9) + 1}호`,
    },
    deliveryMemo: i % 4 === 0 ? '문 앞에 놓아주세요' : '',
    couponIds: [],
    amount: subtotal + shippingFee,
    status,
    createdAt,
  };
});
