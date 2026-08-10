/**
 * 쿠폰 계산 규칙 (순수 함수 — 컴포넌트/슬라이스 어디서든 재사용)
 *
 *  · percent  : 주문금액의 n% 할인, maxDiscount로 상한
 *  · amount   : 정액 할인
 *  · shipping : 배송비만큼 할인 (배송비가 0이면 가치 없음)
 *  · stackable=false 쿠폰은 다른 상품할인 쿠폰과 함께 쓸 수 없습니다.
 */

export const isExpired = (coupon, now = Date.now()) =>
  new Date(coupon.expiresAt).getTime() < now;

export function couponUnavailableReason(coupon, payTotal, shippingFee, now = Date.now()) {
  if (isExpired(coupon, now)) return '유효기간이 지났어요';
  if (payTotal < coupon.minAmount)
    return `${(coupon.minAmount - payTotal).toLocaleString('ko-KR')}원 더 담으면 사용 가능해요`;
  if (coupon.scope === 'shipping' && shippingFee === 0) return '이미 무료배송이라 적용할 수 없어요';
  return null;
}

export function calcCouponDiscount(coupon, payTotal, shippingFee) {
  if (couponUnavailableReason(coupon, payTotal, shippingFee)) return 0;

  switch (coupon.type) {
    case 'percent':
      return Math.min(Math.floor((payTotal * coupon.value) / 100), coupon.maxDiscount);
    case 'amount':
      return Math.min(coupon.value, payTotal);
    case 'shipping':
      return Math.min(coupon.value, shippingFee);
    default:
      return 0;
  }
}

/** 선택된 쿠폰 조합이 규칙에 어긋나지 않는지 검사 */
export function isCombinationValid(coupons) {
  const itemCoupons = coupons.filter((c) => c.scope !== 'shipping');
  const hasExclusive = itemCoupons.some((c) => !c.stackable);
  return !(hasExclusive && itemCoupons.length > 1);
}

/**
 * 사용 가능한 쿠폰 중 할인액이 가장 큰 조합을 찾습니다.
 * (단독 사용 쿠폰 1장) vs (중복 가능 쿠폰 전부) 를 비교 — 실무에서 가장 흔한 규칙.
 */
export function findBestCoupons(coupons, payTotal, shippingFee) {
  const usable = coupons.filter((c) => !couponUnavailableReason(c, payTotal, shippingFee));
  const shipping = usable.filter((c) => c.scope === 'shipping');
  const item = usable.filter((c) => c.scope !== 'shipping');

  const stackables = item.filter((c) => c.stackable);
  const exclusives = item.filter((c) => !c.stackable);

  const stackTotal = stackables.reduce(
    (sum, c) => sum + calcCouponDiscount(c, payTotal, shippingFee),
    0,
  );
  const bestExclusive = exclusives.reduce(
    (best, c) => {
      const value = calcCouponDiscount(c, payTotal, shippingFee);
      return value > best.value ? { value, coupon: c } : best;
    },
    { value: 0, coupon: null },
  );

  const picked =
    bestExclusive.value > stackTotal ? [bestExclusive.coupon].filter(Boolean) : stackables;

  return [...picked, ...shipping].map((c) => c.id);
}

/** 적용된 쿠폰들의 최종 할인 내역 */
export function summarizeCoupons(coupons, appliedIds, payTotal, shippingFee) {
  const applied = coupons.filter((c) => appliedIds.includes(c.id));
  const itemDiscount = applied
    .filter((c) => c.scope !== 'shipping')
    .reduce((sum, c) => sum + calcCouponDiscount(c, payTotal, shippingFee), 0);
  const shippingDiscount = applied
    .filter((c) => c.scope === 'shipping')
    .reduce((sum, c) => sum + calcCouponDiscount(c, payTotal, shippingFee), 0);

  return {
    applied,
    itemDiscount: Math.min(itemDiscount, payTotal),
    shippingDiscount: Math.min(shippingDiscount, shippingFee),
  };
}
