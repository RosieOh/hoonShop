import { useEffect, useState } from 'react';
import { Sparkles, TicketPercent } from 'lucide-react';
import Sheet from '@/components/common/Sheet';
import Button from '@/components/common/Button';
import { useGetCouponsQuery } from './promoApi';
import {
  calcCouponDiscount,
  couponUnavailableReason,
  findBestCoupons,
  isCombinationValid,
} from './couponRules';
import { cn, formatDate, formatPrice } from '@/utils/format';

const typeLabel = (c) =>
  c.type === 'percent' ? `${c.value}%` : c.type === 'shipping' ? '배송비' : `${formatPrice(c.value)}원`;

/**
 * 쿠폰 선택 시트.
 * 사용 불가 쿠폰도 숨기지 않고 "왜 못 쓰는지"와 함께 보여줍니다 —
 * 목록에서 사라지면 사용자는 쿠폰이 없어졌다고 오해합니다.
 */
export default function CouponSheet({ open, onClose, payTotal, shippingFee, applied, onApply }) {
  const { data, isLoading } = useGetCouponsQuery(undefined, { skip: !open });
  const coupons = data?.items ?? [];
  const [draft, setDraft] = useState(applied);

  useEffect(() => {
    if (open) setDraft(applied);
  }, [open, applied]);

  const toggle = (coupon) => {
    setDraft((prev) => {
      const next = prev.includes(coupon.id)
        ? prev.filter((id) => id !== coupon.id)
        : [...prev, coupon.id];
      const picked = coupons.filter((c) => next.includes(c.id));
      // 중복 불가 쿠폰을 새로 고르면 기존 상품할인 쿠폰을 대체합니다.
      if (!isCombinationValid(picked)) {
        return [
          coupon.id,
          ...next.filter((id) => coupons.find((c) => c.id === id)?.scope === 'shipping' && id !== coupon.id),
        ];
      }
      return next;
    });
  };

  const draftDiscount = coupons
    .filter((c) => draft.includes(c.id))
    .reduce((sum, c) => sum + calcCouponDiscount(c, payTotal, shippingFee), 0);

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="쿠폰 선택"
      description="가장 이득인 조합을 자동으로 찾아드려요"
      side="bottom"
      footer={
        <div className="space-y-3">
          <div className="flex items-baseline justify-between">
            <span className="text-[14px] text-ink-soft">적용 할인</span>
            <span className="tnum text-[20px] font-semibold text-sale">
              −{formatPrice(draftDiscount)}원
            </span>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              icon={Sparkles}
              onClick={() => setDraft(findBestCoupons(coupons, payTotal, shippingFee))}
              className="flex-1"
            >
              최대 할인
            </Button>
            <Button
              onClick={() => {
                onApply(draft);
                onClose();
              }}
              className="flex-1"
            >
              적용하기
            </Button>
          </div>
        </div>
      }
    >
      <div className="space-y-2.5 px-5 py-5">
        {isLoading && <p className="py-8 text-center text-[13px] text-ink-soft">쿠폰을 불러오는 중…</p>}

        {coupons.map((coupon) => {
          const reason = couponUnavailableReason(coupon, payTotal, shippingFee);
          const checked = draft.includes(coupon.id);
          const discount = calcCouponDiscount(coupon, payTotal, shippingFee);

          return (
            <label
              key={coupon.id}
              className={cn(
                'flex cursor-pointer items-center gap-3.5 rounded-md border p-4 transition-colors',
                reason && 'cursor-not-allowed opacity-55',
                checked ? 'border-primary bg-primary-soft' : 'border-line hover:border-line-strong',
              )}
            >
              <input
                type="checkbox"
                checked={checked}
                disabled={Boolean(reason)}
                onChange={() => toggle(coupon)}
                className="size-5 shrink-0 accent-[#7A3B52]"
              />

              <span
                className={cn(
                  'font-display grid size-14 shrink-0 place-items-center rounded-sm text-[17px] font-semibold',
                  checked ? 'bg-primary text-on-primary' : 'bg-canvas text-primary',
                )}
                aria-hidden="true"
              >
                {coupon.type === 'shipping' ? <TicketPercent size={20} /> : typeLabel(coupon)}
              </span>

              <span className="min-w-0 flex-1">
                <span className="block text-[15px] font-medium">{coupon.name}</span>
                <span className="mt-0.5 block text-[12px] text-ink-soft">
                  {coupon.minAmount > 0 ? `${formatPrice(coupon.minAmount)}원 이상 · ` : ''}
                  {formatDate(coupon.expiresAt)}까지
                  {coupon.stackable ? ' · 중복 사용 가능' : ' · 단독 사용'}
                </span>
                {reason ? (
                  <span className="mt-1 block text-[12px] font-medium text-ink-soft">{reason}</span>
                ) : (
                  <span className="tnum mt-1 block text-[12px] font-semibold text-sale">
                    −{formatPrice(discount)}원 할인
                  </span>
                )}
              </span>
            </label>
          );
        })}
      </div>
    </Sheet>
  );
}
