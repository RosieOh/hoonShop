import { Link, Navigate, useLocation } from 'react-router-dom';
import { Check, Copy } from 'lucide-react';
import Button from '@/components/common/Button';
import { useToast } from '@/components/common/Toast';
import { formatPrice } from '@/utils/format';

const STEPS = ['결제 완료', '제작 중', '발송', '배송 완료'];

export default function OrderCompletePage() {
  const { state } = useLocation();
  const { toast } = useToast();

  // 직접 URL로 들어온 경우 보여줄 주문 정보가 없습니다.
  if (!state?.order) return <Navigate to="/" replace />;

  const { order, receipt } = state;

  const copyOrderId = async () => {
    try {
      await navigator.clipboard.writeText(order.id);
      toast('주문번호를 복사했어요');
    } catch {
      toast('복사에 실패했어요. 직접 선택해 복사해 주세요.', { tone: 'error' });
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6">
      <div className="text-center">
        <span className="hs-pop mx-auto flex size-16 items-center justify-center rounded-full bg-success-soft text-success">
          <Check size={30} strokeWidth={2.2} aria-hidden="true" />
        </span>

        <h1 className="font-display mt-6 text-[36px] leading-tight font-semibold">
          주문이 완료됐어요
        </h1>
        <p className="mt-3 text-[15px] text-ink-soft">
          지금부터 한 알씩 꿰기 시작합니다. 완성되면 문자로 알려드릴게요.
        </p>
      </div>

      <div className="mt-10 rounded-md border border-line bg-surface p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-5">
          <div>
            <p className="eyebrow">Order number</p>
            <p className="tnum mt-1.5 text-[18px] font-semibold">{order.id}</p>
          </div>
          <button
            type="button"
            onClick={copyOrderId}
            className="flex h-11 items-center gap-1.5 rounded-pill border border-line px-4 text-[13px] transition-colors hover:border-ink"
          >
            <Copy size={14} aria-hidden="true" />
            주문번호 복사
          </button>
        </div>

        <dl className="space-y-3 py-5 text-[14px]">
          <div className="flex justify-between gap-4">
            <dt className="text-ink-soft">결제 금액</dt>
            <dd className="tnum text-[17px] font-semibold">{formatPrice(order.amount)}원</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ink-soft">결제 수단</dt>
            <dd>{receipt?.method === 'CARD' ? '신용·체크카드' : receipt?.method}</dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-ink-soft">받는 분</dt>
            <dd className="text-right">
              {order.shippingAddress.recipient} · {order.shippingAddress.phone}
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="shrink-0 text-ink-soft">배송지</dt>
            <dd className="text-right leading-snug">
              ({order.shippingAddress.zipcode}) {order.shippingAddress.address1}{' '}
              {order.shippingAddress.address2}
            </dd>
          </div>
        </dl>

        <ol className="flex items-center border-t border-line pt-5" aria-label="배송 진행 단계">
          {STEPS.map((label, i) => (
            <li key={label} className="flex flex-1 items-center last:flex-none">
              <div className="flex flex-col items-center gap-1.5">
                <span
                  className={`grid size-7 place-items-center rounded-full text-[11px] font-semibold ${
                    i === 0 ? 'bg-success text-white' : 'bg-canvas text-ink-faint'
                  }`}
                >
                  {i === 0 ? <Check size={13} aria-hidden="true" /> : i + 1}
                </span>
                <span
                  className={`text-[11px] whitespace-nowrap ${i === 0 ? 'font-medium text-ink' : 'text-ink-faint'}`}
                >
                  {label}
                </span>
              </div>
              {i < STEPS.length - 1 && (
                <span className="mb-5 h-px flex-1 bg-line" aria-hidden="true" />
              )}
            </li>
          ))}
        </ol>
      </div>

      <ul className="mt-8 space-y-2 rounded-md bg-canvas p-5 text-[13px] leading-relaxed text-ink-soft">
        <li>· 주문 제작 상품이라 결제 후 24시간이 지나면 취소가 어려울 수 있어요.</li>
        <li>· 배송 조회는 마이페이지 &gt; 주문 내역에서 확인할 수 있습니다.</li>
      </ul>

      <div className="mt-8 flex flex-col gap-2 sm:flex-row">
        <Button to="/mypage" variant="outline" size="lg" className="flex-1">
          주문 내역 보기
        </Button>
        <Button to="/products" size="lg" className="flex-1">
          쇼핑 계속하기
        </Button>
      </div>

      <p className="mt-6 text-center text-[13px] text-ink-soft">
        문의가 있다면{' '}
        <Link to="/help/faq" className="underline underline-offset-4 hover:text-ink">
          고객센터
        </Link>
        로 연락 주세요.
      </p>
    </div>
  );
}
