import { useState } from 'react';
import { Camera, ThumbsUp } from 'lucide-react';
import Rating from '@/components/common/Rating';
import { Skeleton } from '@/components/common/Skeleton';
import { useGetReviewsQuery } from './reviewApi';
import ReviewForm from './ReviewForm';
import { cn, formatRelative } from '@/utils/format';

const SORTS = [
  { id: 'recent', label: '최신순' },
  { id: 'helpful', label: '도움순' },
];

export default function ReviewSection({ productId }) {
  const [sort, setSort] = useState('recent');
  const [writing, setWriting] = useState(false);
  const { data, isLoading } = useGetReviewsQuery({ productId, sort });

  if (isLoading) {
    return (
      <div className="space-y-4 py-6">
        <Skeleton className="h-28 w-full rounded-md" />
        <Skeleton className="h-20 w-full rounded-md" />
      </div>
    );
  }

  const { items = [], average = 0, total = 0, distribution = [] } = data ?? {};
  const max = Math.max(1, ...distribution);

  return (
    <div className="py-2">
      {/* 요약 */}
      <div className="flex flex-col gap-6 rounded-md bg-canvas p-6 sm:flex-row sm:items-center">
        <div className="text-center sm:w-40">
          <p className="tnum font-display text-[44px] leading-none font-semibold">
            {average.toFixed(1)}
          </p>
          <Rating value={average} showValue={false} size={15} className="mt-2 justify-center" />
          <p className="mt-1.5 text-[12px] text-ink-soft">리뷰 {total}개</p>
        </div>

        <ul className="flex-1 space-y-1.5">
          {[5, 4, 3, 2, 1].map((score) => {
            const count = distribution[score - 1] ?? 0;
            return (
              <li key={score} className="flex items-center gap-2.5">
                <span className="tnum w-6 text-[12px] text-ink-soft">{score}점</span>
                <span className="h-1.5 flex-1 overflow-hidden rounded-pill bg-line">
                  <span
                    className="block h-full rounded-pill bg-accent"
                    style={{ width: `${(count / max) * 100}%` }}
                  />
                </span>
                <span className="tnum w-8 text-right text-[12px] text-ink-soft">{count}</span>
              </li>
            );
          })}
        </ul>
      </div>

      <div className="mt-6 flex items-center justify-between gap-3">
        <div className="flex gap-1">
          {SORTS.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => setSort(s.id)}
              aria-pressed={sort === s.id}
              className={cn(
                'h-9 rounded-pill px-3.5 text-[13px] transition-colors',
                sort === s.id ? 'bg-ink text-canvas' : 'text-ink-soft hover:bg-canvas',
              )}
            >
              {s.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={() => setWriting(true)}
          className="h-10 rounded-pill border border-line-strong px-4 text-[13px] font-medium transition-colors hover:border-ink"
        >
          리뷰 쓰기
        </button>
      </div>

      <ul className="mt-2 divide-y divide-line">
        {items.map((review) => (
          <li key={review.id} className="py-5">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <span
                  className="font-display grid size-9 shrink-0 place-items-center rounded-full bg-primary-soft text-[15px] font-semibold text-primary"
                  aria-hidden="true"
                >
                  {review.author.slice(0, 1)}
                </span>
                <div>
                  <p className="text-[13px] font-medium">{review.author}</p>
                  <Rating value={review.rating} showValue={false} size={11} className="mt-0.5" />
                </div>
              </div>
              <time className="text-[12px] text-ink-faint" dateTime={review.createdAt}>
                {formatRelative(review.createdAt)}
              </time>
            </div>

            <p className="mt-3 text-[14px] leading-relaxed text-ink">{review.content}</p>

            {review.photoCount > 0 && (
              <ul className="mt-3 flex gap-2" aria-label={`첨부 사진 ${review.photoCount}장`}>
                {Array.from({ length: review.photoCount }, (_, i) => (
                  <li
                    key={i}
                    className="grid size-16 place-items-center rounded-sm bg-surface-sunken text-ink-faint"
                  >
                    <Camera size={16} aria-hidden="true" />
                  </li>
                ))}
              </ul>
            )}

            <div className="mt-3 flex items-center gap-3">
              <span className="rounded-sm bg-canvas px-2 py-1 text-[11px] text-ink-soft">
                {review.option}
              </span>
              <button
                type="button"
                className="flex h-8 items-center gap-1.5 rounded-pill px-2 text-[12px] text-ink-soft transition-colors hover:bg-canvas hover:text-ink"
              >
                <ThumbsUp size={13} aria-hidden="true" />
                도움돼요 {review.helpful}
              </button>
            </div>
          </li>
        ))}
      </ul>

      <ReviewForm productId={productId} open={writing} onClose={() => setWriting(false)} />
    </div>
  );
}
