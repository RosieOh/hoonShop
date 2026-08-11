import { http, HttpResponse, delay } from 'msw';
import {
  ADDRESSES,
  ADMIN_USER,
  COUPONS,
  DEMO_USER,
  ORDER_STATUS,
  PRODUCTS,
  PROMOTIONS,
  QNA,
  REVIEWS,
  SEED_ORDERS,
} from './db';

const LATENCY = [180, 420];
const jitter = () => LATENCY[0] + Math.random() * (LATENCY[1] - LATENCY[0]);

/**
 * 주문은 세션 메모리에 쌓입니다 (새로고침 시 시드 상태로 초기화).
 * 시드 22건은 관리자 화면이 빈 표로 시작하지 않도록 미리 넣어둔 과거 주문입니다.
 * `own: true`인 주문만 고객 마이페이지에 노출됩니다.
 */
const orders = SEED_ORDERS.map((o) => ({ ...o, own: false }));
let orderSeq = 1;

/** 관리자가 수정하는 재고/노출 상태는 이 맵에 덮어씁니다. */
const productOverrides = new Map();
const withOverrides = (p) => ({ ...p, ...(productOverrides.get(p.id) ?? {}) });
const allProducts = () => PRODUCTS.map(withOverrides);

/** 관리자 답변이 달린 문의를 반영하기 위한 오버레이 */
const qnaAnswers = new Map();
const withAnswer = (q) => ({ ...q, ...(qnaAnswers.get(q.id) ?? {}) });

const norm = (s) => (s ?? '').toString().trim().toLowerCase();

function sortProducts(list, sort) {
  const arr = [...list];
  switch (sort) {
    case 'new':
      return arr.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    case 'popular':
      return arr.sort((a, b) => b.soldCount - a.soldCount);
    case 'price_asc':
      return arr.sort((a, b) => (a.salePrice ?? a.price) - (b.salePrice ?? b.price));
    case 'price_desc':
      return arr.sort((a, b) => (b.salePrice ?? b.price) - (a.salePrice ?? a.price));
    case 'review':
      return arr.sort((a, b) => b.reviewCount - a.reviewCount);
    default:
      return arr.sort(
        (a, b) => b.rating * Math.log(b.reviewCount + 1) - a.rating * Math.log(a.reviewCount + 1),
      );
  }
}

export const handlers = [
  /* ------------------------------------------------------------- 인증 --- */
  http.post('/api/auth/login', async ({ request }) => {
    await delay(jitter());
    const { email, password } = await request.json();
    const account = [DEMO_USER, ADMIN_USER].find((u) => u.email === norm(email));

    if (!account || password !== 'hoonshop') {
      return HttpResponse.json(
        { message: '이메일 또는 비밀번호가 올바르지 않습니다.' },
        { status: 401 },
      );
    }
    return HttpResponse.json({
      user: account,
      token: `mock.jwt.${account.role}.access-token`,
      refreshToken: `mock.jwt.${account.role}.refresh-token`,
      expiresIn: 3600,
    });
  }),

  http.post('/api/auth/refresh', async () => {
    await delay(120);
    return HttpResponse.json({ token: 'mock.jwt.access-token.renewed', expiresIn: 3600 });
  }),

  http.get('/api/auth/me', async ({ request }) => {
    await delay(jitter());
    if (!request.headers.get('authorization')) {
      return HttpResponse.json({ message: '인증이 필요합니다.' }, { status: 401 });
    }
    return HttpResponse.json(DEMO_USER);
  }),

  /* ------------------------------------------------------------- 상품 --- */
  http.get('/api/products', async ({ request }) => {
    await delay(jitter());
    const url = new URL(request.url);
    const category = url.searchParams.get('category') ?? 'all';
    const sort = url.searchParams.get('sort') ?? 'recommend';
    const keyword = norm(url.searchParams.get('q'));
    const page = Number(url.searchParams.get('page') ?? 1);
    const size = Number(url.searchParams.get('size') ?? 12);
    const minPrice = Number(url.searchParams.get('minPrice') ?? 0);
    const maxPrice = Number(url.searchParams.get('maxPrice') ?? Infinity);
    const colors = (url.searchParams.get('colors') ?? '').split(',').filter(Boolean);
    const badge = url.searchParams.get('badge');

    let list = allProducts().filter((p) => {
      const effective = p.salePrice ?? p.price;
      if (category !== 'all' && p.category !== category) return false;
      if (effective < minPrice || effective > maxPrice) return false;
      if (colors.length && !p.colorOptions.some((c) => colors.includes(c.id))) return false;
      if (badge && !p.badges.includes(badge)) return false;
      if (keyword && !norm(`${p.name} ${p.description} ${p.category}`).includes(keyword))
        return false;
      return true;
    });

    list = sortProducts(list, sort);
    const start = (page - 1) * size;

    return HttpResponse.json({
      items: list.slice(start, start + size),
      page,
      size,
      total: list.length,
      hasNext: start + size < list.length,
    });
  }),

  http.get('/api/products/:id', async ({ params }) => {
    await delay(jitter());
    const product = allProducts().find((p) => p.id === params.id);
    if (!product) {
      return HttpResponse.json({ message: '상품을 찾을 수 없습니다.' }, { status: 404 });
    }
    const related = allProducts()
      .filter((p) => p.category === product.category && p.id !== product.id)
      .slice(0, 4);
    return HttpResponse.json({ ...product, related });
  }),

  /* ------------------------------------------------------------- 검색 --- */
  http.get('/api/search/suggestions', async ({ request }) => {
    await delay(90);
    const q = norm(new URL(request.url).searchParams.get('q'));
    if (!q) return HttpResponse.json({ keywords: [], products: [] });

    const products = PRODUCTS.filter((p) => norm(p.name).includes(q)).slice(0, 5);
    const keywords = [...new Set(PRODUCTS.flatMap((p) => p.name.split(' ')))]
      .filter((w) => norm(w).includes(q))
      .slice(0, 6);
    return HttpResponse.json({ keywords, products });
  }),

  http.get('/api/search/trending', async () => {
    await delay(120);
    return HttpResponse.json({
      keywords: ['진주 목걸이', '데이지', '폰스트랩', '민트', '커플 팔찌', '발찌', '시드비즈'],
      updatedAt: new Date().toISOString(),
    });
  }),

  /* ------------------------------------------------- 리뷰 · Q&A (CS) --- */
  http.get('/api/reviews', async ({ request }) => {
    await delay(jitter());
    const url = new URL(request.url);
    const productId = url.searchParams.get('productId');
    const sort = url.searchParams.get('sort') ?? 'recent';
    let list = REVIEWS.filter((r) => !productId || r.productId === productId);
    list =
      sort === 'helpful'
        ? [...list].sort((a, b) => b.helpful - a.helpful)
        : [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));

    const summary = list.reduce(
      (acc, r) => {
        acc.total += 1;
        acc.sum += r.rating;
        acc.distribution[r.rating - 1] += 1;
        return acc;
      },
      { total: 0, sum: 0, distribution: [0, 0, 0, 0, 0] },
    );

    return HttpResponse.json({
      items: list,
      average: summary.total ? Number((summary.sum / summary.total).toFixed(1)) : 0,
      total: summary.total,
      distribution: summary.distribution,
    });
  }),

  http.post('/api/reviews', async ({ request }) => {
    await delay(700);
    const body = await request.json();
    return HttpResponse.json(
      { id: `NEW-${Date.now()}`, ...body, createdAt: new Date().toISOString(), helpful: 0 },
      { status: 201 },
    );
  }),

  http.get('/api/qna', async ({ request }) => {
    await delay(jitter());
    const productId = new URL(request.url).searchParams.get('productId');
    return HttpResponse.json({
      items: QNA.filter((q) => !productId || q.productId === productId),
    });
  }),

  /* --------------------------------------------------- 프로모션 · 쿠폰 --- */
  http.get('/api/promotions', async () => {
    await delay(150);
    const midnight = new Date();
    midnight.setHours(24, 0, 0, 0);
    return HttpResponse.json({
      items: PROMOTIONS.map((p) =>
        p.kind === 'timesale' ? { ...p, endsAt: midnight.toISOString() } : p,
      ),
    });
  }),

  http.get('/api/coupons', async () => {
    await delay(jitter());
    return HttpResponse.json({ items: COUPONS });
  }),

  /* ------------------------------------------------------- 주소 · 주문 --- */
  http.get('/api/addresses', async () => {
    await delay(jitter());
    return HttpResponse.json({ items: ADDRESSES });
  }),

  /** 결제 승인 직전 재고 재확인 — 품절/수량 부족을 사전에 잡아냅니다. */
  http.post('/api/orders/validate', async ({ request }) => {
    await delay(500);
    const { items = [] } = await request.json();
    const issues = items.flatMap((item) => {
      const product = allProducts().find((p) => p.id === item.productId);
      if (!product) return [{ productId: item.productId, reason: 'NOT_FOUND', available: 0 }];
      if (product.stock === 0)
        return [{ productId: item.productId, name: product.name, reason: 'SOLD_OUT', available: 0 }];
      if (product.stock < item.quantity)
        return [
          {
            productId: item.productId,
            name: product.name,
            reason: 'INSUFFICIENT',
            available: product.stock,
          },
        ];
      return [];
    });
    return HttpResponse.json({ ok: issues.length === 0, issues });
  }),

  http.post('/api/orders', async ({ request }) => {
    await delay(600);
    const body = await request.json();
    const order = {
      id: `ORD-2026-${String(orderSeq++).padStart(5, '0')}`,
      ...body,
      customer: { name: DEMO_USER.name, email: DEMO_USER.email },
      own: true,
      status: 'PAYMENT_PENDING',
      createdAt: new Date().toISOString(),
    };
    orders.push(order);
    return HttpResponse.json(order, { status: 201 });
  }),

  /** 마이페이지는 이번 세션에서 내가 만든 주문만 봅니다. */
  http.get('/api/orders', async () => {
    await delay(jitter());
    return HttpResponse.json({ items: orders.filter((o) => o.own).reverse() });
  }),

  http.get('/api/orders/:id', async ({ params }) => {
    await delay(jitter());
    const order = orders.find((o) => o.id === params.id);
    return order
      ? HttpResponse.json(order)
      : HttpResponse.json({ message: '주문 내역이 없습니다.' }, { status: 404 });
  }),

  /* ------------------------------------------------------------- 결제 --- */
  http.post('/api/payments/confirm', async ({ request }) => {
    await delay(1100);
    const body = await request.json();

    // 데모: 카드번호 끝 4자리가 0000이면 승인 거절 시나리오를 재현합니다.
    if (body.card?.number?.replace(/\D/g, '').endsWith('0000')) {
      return HttpResponse.json(
        { code: 'CARD_DECLINED', message: '카드사 승인이 거절되었습니다. 다른 결제수단을 이용해 주세요.' },
        { status: 402 },
      );
    }

    const order = orders.find((o) => o.id === body.orderId);
    if (order) order.status = 'PAID';

    return HttpResponse.json({
      orderId: body.orderId,
      paymentKey: `PAY_${Math.random().toString(36).slice(2, 12).toUpperCase()}`,
      method: body.method,
      amount: body.amount,
      approvedAt: new Date().toISOString(),
      status: 'DONE',
    });
  }),

  /* --------------------------------------------------------- 관리자 --- */

  /**
   * 대시보드 집계.
   * 실서비스라면 서버가 계산해 내려줄 값들입니다 — 프론트에서 전 주문을 받아
   * 합산하면 주문이 늘수록 느려지고, 페이지네이션과도 충돌합니다.
   */
  http.get('/api/admin/stats', async () => {
    await delay(jitter());

    const now = new Date();
    const dayKey = (d) => new Date(d).toISOString().slice(0, 10);
    const todayKey = dayKey(now);
    const valid = orders.filter((o) => o.status !== 'CANCELLED');

    // 최근 14일 일별 매출 (주문이 없는 날도 0으로 채워 축이 끊기지 않게)
    const dailyRevenue = Array.from({ length: 14 }, (_, i) => {
      const d = new Date(now.getTime() - (13 - i) * 86_400_000);
      const key = dayKey(d);
      const dayOrders = valid.filter((o) => dayKey(o.createdAt) === key);
      return {
        date: key,
        revenue: dayOrders.reduce((sum, o) => sum + o.amount, 0),
        orders: dayOrders.length,
      };
    });

    const statusCounts = ORDER_STATUS.map((s) => ({
      ...s,
      count: orders.filter((o) => o.status === s.id).length,
    }));

    const lowStock = allProducts().filter((p) => p.stock <= 5);
    const revenueTotal = valid.reduce((sum, o) => sum + o.amount, 0);
    const todayOrders = valid.filter((o) => dayKey(o.createdAt) === todayKey);

    // 상품별 판매 수량 상위 5
    const soldByProduct = new Map();
    valid.forEach((o) =>
      o.items.forEach((it) => {
        const prev = soldByProduct.get(it.productId) ?? { name: it.name, quantity: 0, revenue: 0 };
        soldByProduct.set(it.productId, {
          name: it.name,
          quantity: prev.quantity + it.quantity,
          revenue: prev.revenue + it.price * it.quantity,
        });
      }),
    );

    return HttpResponse.json({
      todayRevenue: todayOrders.reduce((sum, o) => sum + o.amount, 0),
      todayOrders: todayOrders.length,
      revenueTotal,
      orderTotal: valid.length,
      averageOrderValue: valid.length ? Math.round(revenueTotal / valid.length) : 0,
      needsAction: orders.filter((o) => o.status === 'PAID' || o.status === 'MAKING').length,
      lowStockCount: lowStock.length,
      soldOutCount: allProducts().filter((p) => p.stock === 0).length,
      unansweredQna: QNA.map(withAnswer).filter((q) => !q.answer).length,
      dailyRevenue,
      statusCounts,
      topProducts: [...soldByProduct.values()].sort((a, b) => b.quantity - a.quantity).slice(0, 5),
      lowStock: lowStock.map((p) => ({ id: p.id, name: p.name, stock: p.stock })),
    });
  }),

  http.get('/api/admin/orders', async ({ request }) => {
    await delay(jitter());
    const url = new URL(request.url);
    const status = url.searchParams.get('status');
    const keyword = norm(url.searchParams.get('q'));

    const items = orders
      .filter((o) => (!status || status === 'ALL' ? true : o.status === status))
      .filter((o) =>
        keyword
          ? norm(`${o.id} ${o.customer?.name} ${o.customer?.email}`).includes(keyword)
          : true,
      )
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));

    return HttpResponse.json({ items, total: items.length });
  }),

  http.patch('/api/admin/orders/:id', async ({ params, request }) => {
    await delay(360);
    const { status } = await request.json();
    const order = orders.find((o) => o.id === params.id);
    if (!order) {
      return HttpResponse.json({ message: '주문을 찾을 수 없습니다.' }, { status: 404 });
    }
    if (order.status === 'DELIVERED' || order.status === 'CANCELLED') {
      return HttpResponse.json(
        { message: '이미 종료된 주문은 상태를 바꿀 수 없습니다.' },
        { status: 409 },
      );
    }
    order.status = status;
    return HttpResponse.json(order);
  }),

  http.get('/api/admin/products', async ({ request }) => {
    await delay(jitter());
    const keyword = norm(new URL(request.url).searchParams.get('q'));
    const items = allProducts()
      .filter((p) => (keyword ? norm(`${p.id} ${p.name}`).includes(keyword) : true))
      .sort((a, b) => a.stock - b.stock);
    return HttpResponse.json({ items, total: items.length });
  }),

  http.patch('/api/admin/products/:id', async ({ params, request }) => {
    await delay(320);
    const patch = await request.json();
    const base = PRODUCTS.find((p) => p.id === params.id);
    if (!base) {
      return HttpResponse.json({ message: '상품을 찾을 수 없습니다.' }, { status: 404 });
    }
    if (patch.stock != null && (patch.stock < 0 || patch.stock > 9999)) {
      return HttpResponse.json({ message: '재고는 0~9999 사이여야 합니다.' }, { status: 400 });
    }
    productOverrides.set(params.id, { ...(productOverrides.get(params.id) ?? {}), ...patch });
    return HttpResponse.json(withOverrides(base));
  }),

  http.get('/api/admin/inquiries', async ({ request }) => {
    await delay(jitter());
    const onlyOpen = new URL(request.url).searchParams.get('open') === 'true';
    const items = QNA.map(withAnswer)
      .map((q) => ({
        ...q,
        productName: PRODUCTS.find((p) => p.id === q.productId)?.name ?? q.productId,
      }))
      .filter((q) => (onlyOpen ? !q.answer : true))
      .sort((a, b) => Number(Boolean(a.answer)) - Number(Boolean(b.answer)));
    return HttpResponse.json({ items, total: items.length });
  }),

  http.post('/api/admin/inquiries/:id/answer', async ({ params, request }) => {
    await delay(420);
    const { answer } = await request.json();
    if (!answer?.trim()) {
      return HttpResponse.json({ message: '답변 내용을 입력해 주세요.' }, { status: 400 });
    }
    qnaAnswers.set(params.id, { answer: answer.trim(), answeredAt: new Date().toISOString() });
    return HttpResponse.json({ id: params.id, answer: answer.trim() });
  }),
];
