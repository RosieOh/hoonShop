import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle2, Lock, MessageCircleQuestion } from 'lucide-react';
import Button from '@/components/common/Button';
import EmptyState from '@/components/common/EmptyState';
import { Skeleton } from '@/components/common/Skeleton';
import { useToast } from '@/components/common/Toast';
import { useAnswerInquiryMutation, useGetInquiriesQuery } from '@/features/admin/adminApi';
import { cn, formatDate } from '@/utils/format';

const TEMPLATES = [
  '안녕하세요, 훈샵입니다. 문의 주셔서 감사합니다.',
  '수령 후 7일 이내 미착용 상태라면 교환·반품이 가능합니다.',
  '선물 포장은 주문 시 배송 메모에 남겨주시면 무료로 진행해 드립니다.',
];

export default function AdminInquiries() {
  const [searchParams, setSearchParams] = useSearchParams();
  const onlyOpen = searchParams.get('open') === 'true';

  const { data, isLoading, isFetching } = useGetInquiriesQuery(onlyOpen ? { open: 'true' } : {});
  const items = data?.items ?? [];

  return (
    <div className="mx-auto max-w-[880px]">
      <header className="mb-5">
        <p className="eyebrow">Support</p>
        <h1 className="mt-2 text-[26px] leading-none font-semibold">문의 관리</h1>
      </header>

      <label className="flex w-fit cursor-pointer items-center gap-2.5 rounded-pill border border-line bg-surface py-2 pr-4 pl-3 text-[13px]">
        <input
          type="checkbox"
          checked={onlyOpen}
          onChange={(e) => setSearchParams(e.target.checked ? { open: 'true' } : {}, { replace: true })}
          className="size-4 accent-[#7A3B52]"
        />
        미답변만 보기
      </label>

      <p className="tnum mt-3 text-[13px] text-ink-soft" aria-live="polite">
        {isLoading ? '불러오는 중…' : `${data?.total ?? 0}건`}
      </p>

      {isLoading ? (
        <div className="mt-3 space-y-2">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-28 w-full rounded-md" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={CheckCircle2}
          title="답변을 기다리는 문의가 없어요"
          description="새 문의가 들어오면 사이드바 배지로 알려드립니다."
        />
      ) : (
        <ul
          className={cn(
            'mt-3 space-y-2 transition-opacity duration-200',
            isFetching && !isLoading && 'opacity-60',
          )}
        >
          {items.map((qna) => (
            <InquiryCard key={qna.id} qna={qna} />
          ))}
        </ul>
      )}
    </div>
  );
}

function InquiryCard({ qna }) {
  const { toast } = useToast();
  const [answerInquiry, { isLoading }] = useAnswerInquiryMutation();
  const [draft, setDraft] = useState('');
  const [open, setOpen] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!draft.trim()) return;
    try {
      await answerInquiry({ id: qna.id, answer: draft.trim() }).unwrap();
      toast('답변을 등록했어요');
      setDraft('');
      setOpen(false);
    } catch (err) {
      toast(err?.data?.message ?? '답변을 등록하지 못했어요.', { tone: 'error' });
    }
  };

  return (
    <li className="rounded-md border border-line bg-surface p-5">
      <div className="flex flex-wrap items-center gap-2">
        <span
          className={cn(
            'rounded-pill px-2.5 py-1 text-[11px] font-medium',
            qna.answer ? 'bg-success-soft text-success' : 'bg-primary-soft text-primary',
          )}
        >
          {qna.answer ? '답변 완료' : '답변 대기'}
        </span>
        {qna.secret && (
          <span className="flex items-center gap-1 text-[11px] text-ink-faint">
            <Lock size={11} aria-hidden="true" />
            비밀글
          </span>
        )}
        <span className="text-[12px] text-ink-faint">
          {qna.author} · {formatDate(qna.createdAt)}
        </span>
        <span className="ml-auto max-w-40 truncate text-[12px] text-ink-soft">
          {qna.productName}
        </span>
      </div>

      <p className="mt-3 text-[14px] leading-relaxed">{qna.question}</p>

      {qna.answer ? (
        <p className="mt-3 rounded-md bg-canvas p-4 text-[13px] leading-relaxed text-ink-soft">
          <span className="mb-1 block text-[11px] font-semibold text-ink">훈샵 답변</span>
          {qna.answer}
        </p>
      ) : open ? (
        <form onSubmit={submit} className="mt-3">
          <label htmlFor={`answer-${qna.id}`} className="sr-only">
            답변 내용
          </label>
          <textarea
            id={`answer-${qna.id}`}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            autoFocus
            rows={3}
            placeholder="고객이 다시 묻지 않도록, 결론부터 적어주세요."
            className="w-full resize-y rounded-md border border-line bg-surface p-3 text-[14px] outline-none focus:border-primary"
          />

          {/* 자주 쓰는 문구 — 매번 처음부터 쓰지 않도록 */}
          <div className="mt-2 flex flex-wrap gap-1.5">
            {TEMPLATES.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setDraft((prev) => (prev ? `${prev} ${t}` : t))}
                className="max-w-full truncate rounded-pill border border-line px-3 py-1.5 text-[12px] text-ink-soft transition-colors hover:border-ink hover:text-ink"
              >
                {t.length > 24 ? `${t.slice(0, 24)}…` : t}
              </button>
            ))}
          </div>

          <div className="mt-3 flex gap-2">
            <Button type="submit" size="sm" loading={isLoading} disabled={!draft.trim()}>
              답변 등록
            </Button>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => {
                setDraft('');
                setOpen(false);
              }}
            >
              취소
            </Button>
          </div>
        </form>
      ) : (
        <Button
          size="sm"
          variant="outline"
          icon={MessageCircleQuestion}
          className="mt-3"
          onClick={() => setOpen(true)}
        >
          답변하기
        </Button>
      )}
    </li>
  );
}
