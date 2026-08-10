import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Check, TicketPercent, TriangleAlert } from 'lucide-react';
import Button from '@/components/common/Button';
import { useToast } from '@/components/common/Toast';
import OrderForm from '@/features/order/OrderForm';
import PaymentModule from '@/features/payment/PaymentModule';
import CouponSheet from '@/features/marketing/CouponSheet';
import { useCreateOrderMutation, useValidateStockMutation } from '@/features/order/orderApi';
import { useGetCouponsQuery } from '@/features/marketing/promoApi';
import { summarizeCoupons } from '@/features/marketing/couponRules';
import { applyCoupons, clearCoupons } from '@/features/marketing/promoSlice';
import { confirmPayment, resetPayment, selectCanPay } from '@/features/payment/paymentSlice';
import { clearCart, selectCartSummary, selectSelectedItems } from '@/features/cart/cartSlice';
import { ORDER_STEPS, orderCompleted, resetOrder, setStockIssues } from '@/features/order/orderSlice';
import { validateForm } from '@/utils/validate';
import { cn, formatPrice } from '@/utils/format';

export default function CheckoutPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { toast } = useToast();

  const items = useSelector(selectSelectedItems);
  const summary = useSelector(selectCartSummary);
  const shipping = useSelector((s) => s.order.shippingAddress);
  const memo = useSelector((s) => s.order.deliveryMemo);
  const stockIssues = useSelector((s) => s.order.stockIssues);
  const appliedIds = useSelector((s) => s.promotion.appliedCouponIds);
  const paymentMethod = useSelector((s) => s.payment.method);
  const paymentStatus = useSelector((s) => s.payment.status);
  const canPay = useSelector(selectCanPay);

  const { data: couponData } = useGetCouponsQuery();
  const [validateStock, { isLoading: validating }] = useValidateStockMutation();
  const [createOrder, { isLoading: creating }] = useCreateOrderMutation();

  const [errors, setErrors] = useState({});
  const [couponOpen, setCouponOpen] = useState(false);
  const [card, setCard] = useState({});
  /** 결제 성공 직후에는 장바구니가 비므로, 아래 빈 장바구니 가드가 완료 페이지 이동을 가로채지 못하게 합니다. */
  const [settled, setSettled] = useState(false);

  useEffect(() => {
    dispatch(resetPayment());
    dispatch(resetOrder());
    return () => {
      dispatch(clearCoupons());
    };
  }, [dispatch]);

  // 선택된 상품이 없으면 주문서에 머무를 이유가 없습니다.
  if (items.length === 0 && !settled) return <Navigate to="/cart" replace />;

  const coupons = couponData?.items ?? [];
  const { itemDiscount, shippingDiscount } = summarizeCoupons(
    coupons,
    appliedIds,
    summary.payTotal,
    summary.shippingFee,
  );

  const finalAmount = Math.max(
    0,
    summary.payTotal - itemDiscount + summary.shippingFee - shippingDiscount,
  );

  const busy = validating || creating || paymentStatus === 'loading';

  const handlePay = async () => {
    // 1) 배송지 검증
    const addressErrors = validateForm(shipping, ['recipient', 'phone', 'zipcode', 'address1']);
    if (Object.keys(addressErrors).length) {
      setErrors(addressErrors);
      document.getElementById('shipping-section')?.scrollIntoView({ behavior: 'smooth' });
      toast('배송지 정보를 다시 확인해 주세요.', { tone: 'error' });
      return;
    }

    // 2) 결제 승인 직전 재고 재확인 — 다른 사람이 먼저 사갔을 수 있습니다.
    const stock = await validateStock({
      items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
    }).unwrap();

    if (!stock.ok) {
      dispatch(setStockIssues(stock.issues));
      document.getElementById('stock-alert')?.scrollIntoView({ behavior: 'smooth' });
      toast('재고가 변경된 상품이 있어요.', { tone: 'error' });
      return;
    }
    dispatch(setStockIssues([]));

    // 3) 주문 생성 → 4) 결제 승인
    try {
      const order = await createOrder({
        items: items.map((i) => ({
          productId: i.productId,
          name: i.name,
          options: i.options,
          quantity: i.quantity,
          price: i.salePrice ?? i.price,
        })),
        shippingAddress: shipping,
        deliveryMemo: memo,
        couponIds: appliedIds,
        amount: finalAmount,
      }).unwrap();

      const receipt = await dispatch(
        confirmPayment({
          orderId: order.id,
          method: paymentMethod,
          amount: finalAmount,
          card: paymentMethod === 'CARD' ? card : undefined,
        }),
      ).unwrap();

      setSettled(true);
      dispatch(orderCompleted({ ...order, receipt }));
      dispatch(clearCart());
      navigate('/orders/complete', { replace: true, state: { order, receipt } });
    } catch (err) {
      // 결제 실패 메시지는 paymentSlice가 이미 담고 있어 PaymentModule에 표시됩니다.
      if (!err?.code) toast('주문 처리 중 문제가 발생했어요.', { tone: 'error' });
      document.getElementById('payment-section')?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-40 sm:px-6 sm:pb-16">
      <header className="pt-8 pb-8">
        <h1 className="font-display text-[34px] leading-none font-semibold sm:text-[42px]">
          주문서
        </h1>

        <ol className="mt-6 flex items-center gap-2" aria-label="주문 진행 단계">
          {ORDER_STEPS.map((step, i) => {
            const active = i === 0;
            return (
              <li key={step.id} className="flex items-center gap-2">
                <span
                  className={cn(
                    'flex items-center gap-1.5 rounded-pill px-3 py-1.5 text-[12px]',
                    active ? 'bg-ink font-medium text-canvas' : 'bg-canvas text-ink-faint',
                  )}
                >
                  <span className="tnum">{i + 1}</span>
                  {step.label}
                </span>
                {i < ORDER_STEPS.length - 1 && (
                  <span className="h-px w-4 bg-line" aria-hidden="true" />
                )}
              </li>
            );
          })}
        </ol>
      </header>

      <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
        <div className="space-y-12">
          {stockIssues.length > 0 && (
            <div
              id="stock-alert"
              role="alert"
              className="hs-rise rounded-md border border-danger/30 bg-[#FDF2F1] p-5"
            >
              <p className="flex items-center gap-2 text-[15px] font-semibold text-danger">
                <TriangleAlert size={17} aria-hidden="true" />
                재고가 변경된 상품이 있어요
              </p>
              <ul className="mt-3 space-y-1.5 text-[13px] text-danger">
                {stockIssues.map((issue) => (
                  <li key={issue.productId}>
                    <b>{issue.name ?? issue.productId}</b>
                    {issue.reason === 'SOLD_OUT'
                      ? ' — 품절되었습니다. 장바구니에서 빼주세요.'
                      : ` — ${issue.available}개까지만 주문할 수 있어요.`}
                  </li>
                ))}
              </ul>
              <Button variant="outline" size="sm" to="/cart" className="mt-4">
                장바구니에서 수정하기
              </Button>
            </div>
          )}

          <section id="shipping-section" aria-labelledby="shipping-heading">
            <h2 id="shipping-heading" className="mb-5 text-[20px] font-semibold">
              배송지
            </h2>
            <OrderForm errors={errors} onErrorsChange={setErrors} />
          </section>

          <section aria-labelledby="items-heading">
            <h2 id="items-heading" className="mb-5 text-[20px] font-semibold">
              주문 상품 <span className="text-ink-soft">{summary.count}개</span>
            </h2>
            <ul className="divide-y divide-line rounded-md border border-line px-4">
              {items.map((item) => (
                <li key={item.lineId} className="flex items-center justify-between gap-4 py-3.5">
                  <div className="min-w-0">
                    <p className="truncate text-[14px] font-medium">{item.name}</p>
                    <p className="mt-0.5 text-[12px] text-ink-soft">
                      {[item.options.colorLabel, item.options.size].filter(Boolean).join(' / ')} ·{' '}
                      {item.quantity}개
                    </p>
                  </div>
                  <p className="tnum shrink-0 text-[14px] font-semibold">
                    {formatPrice((item.salePrice ?? item.price) * item.quantity)}원
                  </p>
                </li>
              ))}
            </ul>
          </section>

          <section id="payment-section" aria-labelledby="payment-heading">
            <h2 id="payment-heading" className="mb-5 text-[20px] font-semibold">
              결제
            </h2>
            <PaymentModule onCardChange={setCard} />
          </section>
        </div>

        {/* -------------------------------------------------------- 요약 --- */}
        <aside aria-labelledby="checkout-summary" className="lg:sticky lg:top-24 lg:self-start">
          <div className="rounded-md border border-line bg-surface p-6">
            <h2 id="checkout-summary" className="text-[17px] font-semibold">
              결제 금액
            </h2>

            <button
              type="button"
              onClick={() => setCouponOpen(true)}
              className="mt-4 flex w-full items-center justify-between gap-2 rounded-md border border-line px-4 py-3 text-left transition-colors hover:border-line-strong"
            >
              <span className="flex items-center gap-2 text-[14px]">
                <TicketPercent size={16} className="text-primary" aria-hidden="true" />
                {appliedIds.length > 0 ? `쿠폰 ${appliedIds.length}장 적용됨` : '쿠폰 사용하기'}
              </span>
              <span className="tnum text-[13px] font-semibold text-sale">
                {itemDiscount + shippingDiscount > 0
                  ? `−${formatPrice(itemDiscount + shippingDiscount)}원`
                  : '선택'}
              </span>
            </button>

            <dl className="mt-5 space-y-2.5 text-[14px]">
              <Row label="상품 금액" value={`${formatPrice(summary.listTotal)}원`} />
              {summary.itemDiscount > 0 && (
                <Row label="상품 할인" value={`−${formatPrice(summary.itemDiscount)}원`} sale />
              )}
              {itemDiscount > 0 && (
                <Row label="쿠폰 할인" value={`−${formatPrice(itemDiscount)}원`} sale />
              )}
              <Row
                label="배송비"
                value={summary.shippingFee === 0 ? '무료' : `+${formatPrice(summary.shippingFee)}원`}
              />
              {shippingDiscount > 0 && (
                <Row label="배송비 할인" value={`−${formatPrice(shippingDiscount)}원`} sale />
              )}
            </dl>

            <div className="mt-5 flex items-baseline justify-between border-t border-line pt-5">
              <span className="text-[15px] font-medium">총 결제 금액</span>
              <span className="tnum font-display text-[28px] leading-none font-semibold">
                {formatPrice(finalAmount)}
                <span className="ml-1 text-[15px] font-medium">원</span>
              </span>
            </div>

            <Button
              size="lg"
              full
              className="mt-6 hidden lg:inline-flex"
              onClick={handlePay}
              loading={busy}
              disabled={!canPay}
              icon={canPay && !busy ? Check : undefined}
            >
              {busy ? '처리 중…' : `${formatPrice(finalAmount)}원 결제하기`}
            </Button>

            {!canPay && (
              <p className="mt-2.5 hidden text-center text-[12px] text-ink-soft lg:block">
                필수 약관에 동의하면 결제할 수 있어요
              </p>
            )}
          </div>
        </aside>
      </div>

      {/* 모바일 고정 결제 바 (체크아웃에서는 하단 내비를 숨기므로 bottom-0) */}
      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-canvas/95 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur-md lg:hidden">
        <Button size="lg" full onClick={handlePay} loading={busy} disabled={!canPay}>
          {busy ? '처리 중…' : `${formatPrice(finalAmount)}원 결제하기`}
        </Button>
        {!canPay && (
          <p className="mt-1.5 text-center text-[12px] text-ink-soft">
            필수 약관에 동의하면 결제할 수 있어요
          </p>
        )}
      </div>

      <CouponSheet
        open={couponOpen}
        onClose={() => setCouponOpen(false)}
        payTotal={summary.payTotal}
        shippingFee={summary.shippingFee}
        applied={appliedIds}
        onApply={(ids) => dispatch(applyCoupons(ids))}
      />
    </div>
  );
}

function Row({ label, value, sale }) {
  return (
    <div className="flex justify-between">
      <dt className="text-ink-soft">{label}</dt>
      <dd className={cn('tnum', sale ? 'text-sale' : 'text-ink')}>{value}</dd>
    </div>
  );
}
