import { ChevronDown, Lock, MessageCircleQuestion } from 'lucide-react';
import { Skeleton } from '@/components/common/Skeleton';
import EmptyState from '@/components/common/EmptyState';
import { useGetQnaQuery } from './reviewApi';
import { formatDate } from '@/utils/format';

export default function QnaList({ productId }) {
  const { data, isLoading } = useGetQnaQuery(productId);

  if (isLoading) {
    return (
      <div className="space-y-3 py-6">
        <Skeleton className="h-14 w-full rounded-md" />
        <Skeleton className="h-14 w-full rounded-md" />
      </div>
    );
  }

  const items = data?.items ?? [];

  if (items.length === 0) {
    return (
      <EmptyState
        icon={MessageCircleQuestion}
        title="등록된 문의가 없어요"
        description="궁금한 점이 있다면 편하게 남겨주세요. 영업일 기준 1일 내 답변드립니다."
        actionLabel="문의하기"
        onAction={() => {}}
      />
    );
  }

  return (
    <ul className="divide-y divide-line py-2">
      {items.map((qna) => (
        <li key={qna.id}>
          <details className="group">
            <summary className="flex list-none items-center gap-3 py-4">
              <span
                className={`grid size-7 shrink-0 place-items-center rounded-full text-[12px] font-semibold ${
                  qna.answer ? 'bg-success-soft text-success' : 'bg-canvas text-ink-soft'
                }`}
              >
                {qna.answer ? 'A' : 'Q'}
              </span>

              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-1.5 text-[14px] font-medium">
                  {qna.secret && <Lock size={12} className="shrink-0 text-ink-faint" aria-hidden="true" />}
                  <span className="truncate">{qna.secret ? '비밀글입니다' : qna.question}</span>
                </span>
                <span className="mt-0.5 block text-[12px] text-ink-faint">
                  {qna.author} · {formatDate(qna.createdAt)} ·{' '}
                  {qna.answer ? '답변 완료' : '답변 대기'}
                </span>
              </span>

              <ChevronDown
                size={17}
                className="shrink-0 text-ink-faint transition-transform duration-200 group-open:rotate-180"
                aria-hidden="true"
              />
            </summary>

            <div className="pb-5 pl-10 text-[14px] leading-relaxed">
              {qna.secret ? (
                <p className="text-ink-soft">작성자와 판매자만 볼 수 있는 글입니다.</p>
              ) : (
                <>
                  <p className="text-ink">{qna.question}</p>
                  {qna.answer && (
                    <p className="mt-3 rounded-md bg-canvas p-4 text-ink-soft">{qna.answer}</p>
                  )}
                </>
              )}
            </div>
          </details>
        </li>
      ))}
    </ul>
  );
}
