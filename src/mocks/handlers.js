import { http, HttpResponse, delay } from 'msw';
import {
  ADDRESSES,
  COUPONS,
  DEMO_USER,
  PRODUCTS,
  PROMOTIONS,
  QNA,
  REVIEWS,
} from './db';

const LATENCY = [180, 420];
const jitter = () => LATENCY[0] + Math.random() * (LATENCY[1] - LATENCY[0]);

/** 주문은 세션 메모리에 쌓입니다 (새로고침 시 초기화). */
const orders = [];
let orderSeq = 1;

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
    if (norm(email) !== DEMO_USER.email || password !== 'hoonshop') {
      return HttpResponse.json(
        { message: '이메일 또는 비밀번호가 올바르지 않습니다.' },
        { status: 401 },
      );
    }
    return HttpResponse.json({
      user: DEMO_USER,
      token: 'mock.jwt.access-token',
      refreshToken: 'mock.jwt.refresh-token',
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

    let list = PRODUCTS.filter((p) => {
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
    const product = PRODUCTS.find((p) => p.id === params.id);
    if (!product) {
      return HttpResponse.json({ message: '상품을 찾을 수 없습니다.' }, { status: 404 });
    }
    const related = PRODUCTS.filter(
      (p) => p.category === product.category && p.id !== product.id,
    ).slice(0, 4);
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
      const product = PRODUCTS.find((p) => p.id === item.productId);
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
      status: 'PAYMENT_PENDING',
      createdAt: new Date().toISOString(),
    };
    orders.push(order);
    return HttpResponse.json(order, { status: 201 });
  }),

  http.get('/api/orders', async () => {
    await delay(jitter());
    return HttpResponse.json({ items: [...orders].reverse() });
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
];
