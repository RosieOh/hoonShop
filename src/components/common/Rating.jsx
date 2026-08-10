import { Star } from 'lucide-react';
import { cn } from '@/utils/format';

/** 별점. 색만으로 정보를 전달하지 않도록 숫자 값을 항상 함께 노출합니다. */
export default function Rating({ value = 0, count, size = 14, showValue = true, className }) {
  const rounded = Math.round(value * 2) / 2;

  return (
    <span className={cn('inline-flex items-center gap-1.5', className)}>
      <span className="flex items-center gap-px" aria-hidden="true">
        {[1, 2, 3, 4, 5].map((i) => (
          <Star
            key={i}
            size={size}
            className={i <= rounded ? 'text-accent' : 'text-line-strong'}
            fill="currentColor"
            strokeWidth={0}
          />
        ))}
      </span>
      {showValue && (
        <span className="tnum text-[13px] font-medium text-ink">{value.toFixed(1)}</span>
      )}
      {count != null && (
        <span className="tnum text-[13px] text-ink-soft">({count.toLocaleString('ko-KR')})</span>
      )}
      <span className="sr-only">
        5점 만점에 {value.toFixed(1)}점{count != null ? `, 리뷰 ${count}개` : ''}
      </span>
    </span>
  );
}
