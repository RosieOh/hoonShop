import { Minus, Plus } from 'lucide-react';
import { cn } from '@/utils/format';

/** 수량 조절. 버튼은 44px, 상한/하한 도달 시 disabled로 명확히 알립니다. */
export default function QuantityStepper({
  value,
  onChange,
  min = 1,
  max = 10,
  label = '수량',
  size = 'md',
  className,
}) {
  const box = size === 'sm' ? 'size-9' : 'size-11';

  return (
    <div
      className={cn('inline-flex items-center rounded-pill border border-line bg-surface', className)}
    >
      <button
        type="button"
        className={cn(
          box,
          'flex items-center justify-center rounded-full text-ink transition-colors hover:bg-canvas disabled:text-ink-faint disabled:hover:bg-transparent',
        )}
        onClick={() => onChange(value - 1)}
        disabled={value <= min}
        aria-label={`${label} 1개 줄이기`}
      >
        <Minus size={16} aria-hidden="true" />
      </button>

      <output
        className={cn('tnum min-w-9 text-center text-[15px] font-semibold', size === 'sm' && 'min-w-8')}
        aria-label={`${label} ${value}개`}
      >
        {value}
      </output>

      <button
        type="button"
        className={cn(
          box,
          'flex items-center justify-center rounded-full text-ink transition-colors hover:bg-canvas disabled:text-ink-faint disabled:hover:bg-transparent',
        )}
        onClick={() => onChange(value + 1)}
        disabled={value >= max}
        aria-label={`${label} 1개 늘리기`}
      >
        <Plus size={16} aria-hidden="true" />
      </button>
    </div>
  );
}
