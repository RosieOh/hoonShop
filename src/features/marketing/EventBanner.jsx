import { useDispatch, useSelector } from 'react-redux';
import { X } from 'lucide-react';
import { useCountdown } from '@/hooks/useCountdown';
import { useGetPromotionsQuery } from './promoApi';
import { dismissBanner, selectShowBanner } from './promoSlice';

/** 상단 프로모션 스트립. 닫으면 그 날 하루는 다시 뜨지 않습니다. */
export default function EventBanner() {
  const dispatch = useDispatch();
  const visible = useSelector(selectShowBanner);
  const { data } = useGetPromotionsQuery();

  const timesale = data?.items?.find((p) => p.kind === 'timesale');
  const time = useCountdown(timesale?.endsAt);

  if (!visible || !timesale || time.done) return null;

  return (
    <div className="relative bg-ink text-canvas">
      <div className="mx-auto flex max-w-[1240px] items-center justify-center gap-2.5 px-12 py-2.5 text-center">
        <p className="text-[13px] leading-snug">
          <span className="font-label mr-2 text-[10px] font-semibold tracking-[0.16em] text-accent">
            TIME SALE
          </span>
          {timesale.subtitle}
        </p>
        <p className="tnum font-label hidden text-[13px] font-semibold sm:block" aria-live="off">
          {time.hours}:{time.minutes}:{time.seconds}
        </p>
        <span className="sr-only">
          타임세일 종료까지 {Number(time.hours)}시간 {Number(time.minutes)}분 남았습니다.
        </span>
      </div>

      <button
        type="button"
        onClick={() => dispatch(dismissBanner())}
        aria-label="오늘 하루 이 배너 닫기"
        className="absolute top-1/2 right-2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full text-canvas/70 transition-colors hover:bg-white/10 hover:text-canvas"
      >
        <X size={16} aria-hidden="true" />
      </button>
    </div>
  );
}
