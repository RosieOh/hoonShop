import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ChevronRight, Search } from 'lucide-react';
import StatusBadge from '@/features/admin/StatusBadge';
import EmptyState from '@/components/common/EmptyState';
import { Skeleton } from '@/components/common/Skeleton';
import { useToast } from '@/components/common/Toast';
import { useGetAdminOrdersQuery, useUpdateOrderStatusMutation } from '@/features/admin/adminApi';
import { ORDER_STATUS } from '@/mocks/db';
import { useDebounce } from '@/hooks/useDebounce';
import { cn, formatDate, formatPrice } from '@/utils/format';
import { Receipt } from 'lucide-react';

const TABS = [{ id: 'ALL', label: '전체' }, ...ORDER_STATUS.map((s) => ({ id: s.id, label: s.label }))];

export default function AdminOrders() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { toast } = useToast();

  const status = searchParams.get('status') ?? 'ALL';
  const [keyword, setKeyword] = useState(searchParams.get('q') ?? '');
  const debounced = useDebounce(keyword.trim(), 300);

  // 필터 전체가 캐시 키이자 낙관적 업데이트의 대상이므로 한 객체로 묶어 씁니다.
  const queryArgs = useMemo(
    () => ({ status, ...(debounced ? { q: debounced } : {}) }),
    [status, debounced],
  );

  const { data, isLoading, isFetching } = useGetAdminOrdersQuery(queryArgs);
  const [updateStatus, { isLoading: updating }] = useUpdateOrderStatusMutation();

  const orders = data?.items ?? [];

  const advance = async (order) => {
    const next = ORDER_STATUS.find((s) => s.id === order.status)?.next;
    if (!next) return;
    try {
      await updateStatus({ id: order.id, status: next, queryArgs }).unwrap();
      toast(`${order.id} → ${ORDER_STATUS.find((s) => s.id === next).label}`);
    } catch (err) {
      toast(err?.data?.message ?? '상태를 바꾸지 못했어요.', { tone: 'error' });
    }
  };

  const setStatus = (next) => {
    const params = {};
    if (next !== 'ALL') params.status = next;
    if (keyword.trim()) params.q = keyword.trim();
    setSearchParams(params, { replace: true });
  };

  return (
    <div className="mx-auto max-w-[1120px]">
      <header className="mb-5">
        <p className="eyebrow">Orders</p>
        <h1 className="mt-2 text-[26px] leading-none font-semibold">주문 관리</h1>
      </header>

      {/* 필터는 한 줄에 모아 표 위에 둡니다 — 카드 안에 흩어 놓지 않습니다 */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="scrollbar-none -mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
          <div className="flex w-max gap-1.5">
            {TABS.map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => setStatus(t.id)}
                aria-pressed={status === t.id}
                className={cn(
                  'h-9 rounded-pill border px-3.5 text-[13px] whitespace-nowrap transition-colors',
                  status === t.id
                    ? 'border-ink bg-ink text-canvas'
                    : 'border-line bg-surface text-ink-soft hover:border-line-strong hover:text-ink',
                )}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="relative sm:ml-auto sm:w-64">
          <label htmlFor="order-search" className="sr-only">
            주문번호 또는 주문자 검색
          </label>
          <Search
            size={15}
            className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-ink-faint"
            aria-hidden="true"
          />
          <input
            id="order-search"
            type="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="주문번호 · 주문자"
            className="h-9 w-full rounded-pill border border-line bg-surface pr-3 pl-9 text-[13px] outline-none focus:border-primary"
          />
        </div>
      </div>

      <p className="tnum mt-3 text-[13px] text-ink-soft" aria-live="polite">
        {isLoading ? '불러오는 중…' : `${data?.total ?? 0}건`}
      </p>

      {isLoading ? (
        <div className="mt-3 space-y-2">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-md" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <EmptyState
          icon={Receipt}
          title="조건에 맞는 주문이 없어요"
          description="상태 필터를 바꾸거나 검색어를 지워보세요."
        />
      ) : (
        <div
          className={cn(
            'mt-3 transition-opacity duration-200',
            isFetching && !isLoading && 'opacity-60',
          )}
        >
          {/* 데스크톱: 표 */}
          <div className="hidden overflow-x-auto rounded-md border border-line bg-surface lg:block">
            <table className="w-full text-[13px]">
              <caption className="sr-only">주문 목록</caption>
              <thead>
                <tr className="border-b border-line text-left text-ink-soft">
                  <th scope="col" className="px-4 py-3 font-medium">주문번호</th>
                  <th scope="col" className="px-4 py-3 font-medium">주문자</th>
                  <th scope="col" className="px-4 py-3 font-medium">상품</th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">결제금액</th>
                  <th scope="col" className="px-4 py-3 font-medium">상태</th>
                  <th scope="col" className="px-4 py-3 font-medium">주문일</th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    <span className="sr-only">다음 단계로</span>처리
                  </th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-line/60 last:border-0 hover:bg-canvas">
                    <td className="tnum px-4 py-3 font-medium">{order.id}</td>
                    <td className="px-4 py-3">
                      <span className="block">{order.customer?.name ?? '-'}</span>
                      <span className="block text-[12px] text-ink-faint">
                        {order.customer?.email}
                      </span>
                    </td>
                    <td className="max-w-64 px-4 py-3">
                      <span className="block truncate">{order.items[0]?.name}</span>
                      {order.items.length > 1 && (
                        <span className="block text-[12px] text-ink-faint">
                          외 {order.items.length - 1}종
                        </span>
                      )}
                    </td>
                    <td className="tnum px-4 py-3 text-right font-semibold">
                      {formatPrice(order.amount)}원
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={order.status} />
                    </td>
                    <td className="tnum px-4 py-3 text-ink-soft">{formatDate(order.createdAt)}</td>
                    <td className="px-4 py-3 text-right">
                      <NextStepButton order={order} onAdvance={advance} disabled={updating} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 모바일: 카드 */}
          <ul className="space-y-2 lg:hidden">
            {orders.map((order) => (
              <li key={order.id} className="rounded-md border border-line bg-surface p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="tnum text-[14px] font-semibold">{order.id}</p>
                    <p className="mt-0.5 text-[12px] text-ink-soft">
                      {order.customer?.name} · {formatDate(order.createdAt)}
                    </p>
                  </div>
                  <StatusBadge status={order.status} />
                </div>

                <p className="mt-3 truncate text-[13px]">
                  {order.items[0]?.name}
                  {order.items.length > 1 && (
                    <span className="text-ink-faint"> 외 {order.items.length - 1}종</span>
                  )}
                </p>

                <div className="mt-3 flex items-center justify-between gap-3 border-t border-line pt-3">
                  <span className="tnum text-[15px] font-semibold">
                    {formatPrice(order.amount)}원
                  </span>
                  <NextStepButton order={order} onAdvance={advance} disabled={updating} />
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function NextStepButton({ order, onAdvance, disabled }) {
  const next = ORDER_STATUS.find((s) => s.id === order.status)?.next;

  if (!next) {
    return <span className="text-[12px] text-ink-faint">처리 완료</span>;
  }

  const nextLabel = ORDER_STATUS.find((s) => s.id === next).label;

  return (
    <button
      type="button"
      onClick={() => onAdvance(order)}
      disabled={disabled}
      aria-label={`${order.id} 상태를 ${nextLabel}(으)로 변경`}
      className="inline-flex h-9 items-center gap-1 rounded-pill border border-line-strong px-3 text-[12px] font-medium transition-colors hover:border-ink disabled:opacity-50"
    >
      {nextLabel}
      <ChevronRight size={13} aria-hidden="true" />
    </button>
  );
}
