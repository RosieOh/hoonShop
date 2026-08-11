import { Link } from 'react-router-dom';
import { Ban } from 'lucide-react';
import { formatPrice } from '@/utils/format';

/**
 * 주문 상태 분포 — 순서가 있는 단계이므로 categorical이 아니라 ordinal 램프를 씁니다.
 *
 * 램프는 브랜드 플럼 단일 색조 4단계로, 밝기가 단조 증가하고 가장 밝은 단계도
 * 흰 배경 대비 2:1을 넘습니다 (검증 스크립트 통과).
 * '취소'는 진행 단계가 아니라 예외 상태라 막대에서 빼고 따로 표기합니다 —
 * 퍼널 안에 섞으면 "취소도 하나의 진행 단계"로 잘못 읽힙니다.
 */
const RAMP = ['#CFA3B4', '#B67C93', '#985871', '#7A3B52'];
const STAGES = ['PAID', 'MAKING', 'SHIPPED', 'DELIVERED'];

export default function OrderFunnel({ statusCounts = [], loading }) {
  const stages = STAGES.map((id, i) => {
    const found = statusCounts.find((s) => s.id === id);
    return { id, label: found?.label ?? id, count: found?.count ?? 0, color: RAMP[i] };
  });

  const cancelled = statusCounts.find((s) => s.id === 'CANCELLED');
  const total = stages.reduce((sum, s) => sum + s.count, 0);

  if (loading) {
    return (
      <div className="rounded-md border border-line bg-surface p-5">
        <div className="hs-skeleton h-40 w-full rounded-sm" aria-label="주문 상태를 불러오는 중" />
      </div>
    );
  }

  return (
    <figure className="rounded-md border border-line bg-surface p-5">
      <figcaption className="mb-4">
        <h2 className="text-[15px] font-semibold">주문 상태</h2>
        <p className="mt-1 text-[13px] text-ink-soft">
          진행 중인 주문 <b className="font-semibold text-ink">{formatPrice(total)}건</b>
        </p>
      </figcaption>

      {/* 누적 막대 — 경계는 테두리가 아니라 2px 배경색 간격으로 나눕니다 */}
      <div className="flex h-3 w-full gap-[2px] overflow-hidden rounded-pill" role="presentation">
        {stages.map((s) =>
          s.count > 0 ? (
            <span
              key={s.id}
              className="h-full first:rounded-l-pill last:rounded-r-pill"
              style={{ width: `${(s.count / Math.max(total, 1)) * 100}%`, background: s.color }}
            />
          ) : null,
        )}
        {total === 0 && <span className="h-full w-full rounded-pill bg-surface-sunken" />}
      </div>

      {/* 범례가 곧 표 — 색만으로 정보를 전달하지 않습니다 */}
      <ul className="mt-4 space-y-2">
        {stages.map((s) => (
          <li key={s.id} className="flex items-center gap-2.5 text-[13px]">
            <span
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: s.color }}
              aria-hidden="true"
            />
            <span className="flex-1 text-ink-soft">{s.label}</span>
            <span className="tnum font-semibold">{s.count}건</span>
          </li>
        ))}
      </ul>

      {cancelled?.count > 0 && (
        <p className="mt-4 flex items-center gap-2 border-t border-line pt-3 text-[13px] text-ink-soft">
          <Ban size={13} className="shrink-0" aria-hidden="true" />
          <span className="flex-1">취소 (진행 단계 아님)</span>
          <span className="tnum font-semibold text-ink">{cancelled.count}건</span>
        </p>
      )}

      <Link
        to="/admin/orders"
        className="mt-4 flex h-10 items-center justify-center rounded-pill border border-line text-[13px] transition-colors hover:border-ink"
      >
        주문 관리로 이동
      </Link>
    </figure>
  );
}
