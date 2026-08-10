import { Truck } from 'lucide-react';
import { formatPrice } from '@/utils/format';

/**
 * 무료배송까지 남은 금액.
 * 진행률을 색으로만 표현하지 않고 문장으로도 알려줍니다.
 */
export default function FreeShippingMeter({ summary }) {
  const { payTotal, freeShippingGap, freeShippingThreshold } = summary;
  const percent = Math.min(100, Math.round((payTotal / freeShippingThreshold) * 100));
  const reached = freeShippingGap === 0 && payTotal > 0;

  if (payTotal === 0) return null;

  return (
    <div className="rounded-md bg-canvas px-3.5 py-3">
      <p className="flex items-center gap-1.5 text-[13px]">
        <Truck size={14} className="shrink-0 text-primary" aria-hidden="true" />
        {reached ? (
          <span className="font-medium text-success">무료배송 조건을 채웠어요</span>
        ) : (
          <span>
            <b className="tnum font-semibold text-primary">{formatPrice(freeShippingGap)}원</b> 더
            담으면 무료배송
          </span>
        )}
      </p>

      <div
        className="mt-2 h-1.5 overflow-hidden rounded-pill bg-line"
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label="무료배송 달성률"
      >
        <div
          className="h-full rounded-pill bg-primary transition-[width] duration-500 ease-out-soft"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
