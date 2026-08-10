import { useDispatch, useSelector } from 'react-redux';
import { ShoppingBag } from 'lucide-react';
import Button from '@/components/common/Button';
import EmptyState from '@/components/common/EmptyState';
import CartItem from '@/features/cart/CartItem';
import FreeShippingMeter from '@/features/cart/FreeShippingMeter';
import {
  removeSelected,
  selectCartSummary,
  selectSelectedIds,
  toggleSelectAll,
} from '@/features/cart/cartSlice';
import { formatPrice } from '@/utils/format';

export default function CartPage() {
  const dispatch = useDispatch();
  const items = useSelector((s) => s.cart.items);
  const selected = useSelector(selectSelectedIds);
  const summary = useSelector(selectCartSummary);

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6">
        <EmptyState
          icon={ShoppingBag}
          title="장바구니가 비어 있어요"
          description="마음에 드는 비즈를 골라 담아보세요."
          actionLabel="상품 보러 가기"
          actionTo="/products"
        />
      </div>
    );
  }

  const allSelected = selected.length === items.length;
  const finalTotal = summary.payTotal + summary.shippingFee;

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-16 sm:px-6">
      <h1 className="font-display pt-8 pb-6 text-[34px] leading-none font-semibold sm:text-[42px]">
        장바구니
      </h1>

      <div className="grid gap-10 lg:grid-cols-[1fr_360px]">
        <section aria-label="담은 상품">
          <div className="flex items-center justify-between border-y border-line py-3">
            <label className="flex cursor-pointer items-center gap-2.5 text-[14px]">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={() => dispatch(toggleSelectAll())}
                className="size-5 accent-[#7A3B52]"
              />
              전체 선택 ({selected.length}/{items.length})
            </label>

            <button
              type="button"
              onClick={() => dispatch(removeSelected())}
              disabled={selected.length === 0}
              className="h-10 px-2 text-[13px] text-ink-soft underline underline-offset-4 transition-colors hover:text-danger disabled:text-ink-faint disabled:no-underline"
            >
              선택 삭제
            </button>
          </div>

          <ul className="divide-y divide-line">
            {items.map((item) => (
              <CartItem key={item.lineId} item={item} selected={selected.includes(item.lineId)} />
            ))}
          </ul>
        </section>

        {/* 결제 요약 — 데스크톱은 스티키, 모바일은 하단 고정 바 */}
        <aside aria-labelledby="summary-heading" className="lg:sticky lg:top-24 lg:self-start">
          <div className="rounded-md border border-line bg-surface p-6">
            <h2 id="summary-heading" className="text-[17px] font-semibold">
              결제 예정 금액
            </h2>

            <dl className="mt-5 space-y-2.5 text-[14px]">
              <Row label="상품 금액" value={`${formatPrice(summary.listTotal)}원`} />
              {summary.itemDiscount > 0 && (
                <Row
                  label="상품 할인"
                  value={`−${formatPrice(summary.itemDiscount)}원`}
                  tone="sale"
                />
              )}
              <Row
                label="배송비"
                value={summary.shippingFee === 0 ? '무료' : `+${formatPrice(summary.shippingFee)}원`}
              />
            </dl>

            <div className="mt-5 flex items-baseline justify-between border-t border-line pt-5">
              <span className="text-[15px] font-medium">총 결제 금액</span>
              <span className="tnum font-display text-[28px] leading-none font-semibold">
                {formatPrice(finalTotal)}
                <span className="ml-1 text-[15px] font-medium">원</span>
              </span>
            </div>

            <div className="mt-4">
              <FreeShippingMeter summary={summary} />
            </div>

            <Button
              to="/checkout"
              size="lg"
              full
              className="mt-5"
              disabled={summary.count === 0}
            >
              {summary.count > 0 ? `${summary.count}개 주문하기` : '상품을 선택해 주세요'}
            </Button>

            <Button variant="ghost" to="/products" full className="mt-1.5">
              계속 쇼핑하기
            </Button>
          </div>
        </aside>
      </div>

      {/* 모바일 고정 결제 바 */}
      <div className="fixed inset-x-0 bottom-16 z-20 border-t border-line bg-canvas/95 px-4 py-3 backdrop-blur-md lg:hidden">
        <div className="mb-2 flex items-baseline justify-between">
          <span className="text-[13px] text-ink-soft">총 결제 금액</span>
          <span className="tnum text-[20px] font-semibold">{formatPrice(finalTotal)}원</span>
        </div>
        <Button to="/checkout" full disabled={summary.count === 0}>
          {summary.count > 0 ? `${summary.count}개 주문하기` : '상품을 선택해 주세요'}
        </Button>
      </div>
    </div>
  );
}

function Row({ label, value, tone }) {
  return (
    <div className="flex justify-between">
      <dt className="text-ink-soft">{label}</dt>
      <dd className={`tnum ${tone === 'sale' ? 'text-sale' : 'text-ink'}`}>{value}</dd>
    </div>
  );
}
