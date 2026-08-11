import { useId, useState } from 'react';
import { Table2, BarChart3 } from 'lucide-react';
import { cn, formatCompactWon, formatPrice, formatShortDate } from '@/utils/format';

/**
 * 최근 14일 일별 매출 — 단일 계열 컬럼 차트.
 *
 * 설계 규칙 (데이터 시각화 가이드라인 준수)
 *  · 계열이 하나이므로 모든 막대는 같은 색입니다. 값이 클수록 진하게 칠하면
 *    막대 길이가 이미 말하는 정보를 색으로 중복 인코딩하게 됩니다.
 *  · 막대 두께는 24px 상한, 데이터 끝만 4px 라운드하고 baseline 쪽은 각지게 둡니다.
 *  · 격자선은 실선 hairline. 점선은 "임계값"으로 오해됩니다.
 *  · 값 라벨은 최고치 하루에만 답니다. 전 지점에 숫자를 붙이면 아무도 읽지 않습니다.
 *  · 툴팁은 값을 읽는 유일한 통로가 되면 안 되므로 표 보기를 함께 제공합니다.
 *  · 키보드 포커스도 hover와 같은 정보를 보여줍니다.
 */

const TICK_COUNT = 4;

/** 축 눈금이 1,234,567 같은 값으로 끝나지 않도록 위로 반올림 */
function niceCeil(value) {
  if (value <= 0) return 10_000;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  const normalized = value / magnitude;
  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return step * magnitude;
}

export default function RevenueChart({ data = [], loading, stale }) {
  const [view, setView] = useState('chart');
  const [active, setActive] = useState(null);
  const captionId = useId();

  const max = Math.max(...data.map((d) => d.revenue), 0);
  // 눈금 간격을 먼저 반올림한 뒤 상한을 정합니다.
  // 상한만 반올림하면 0 / 13만 / 25만 / 38만 처럼 눈금 자체가 지저분해집니다.
  const step = niceCeil(max / TICK_COUNT) || 10_000;
  const axisMax = step * TICK_COUNT;
  const peakIndex = data.findIndex((d) => d.revenue === max && max > 0);
  const total = data.reduce((sum, d) => sum + d.revenue, 0);

  const ticks = Array.from({ length: TICK_COUNT + 1 }, (_, i) => step * i);

  return (
    <figure
      className={cn(
        'rounded-md border border-line bg-surface p-5 transition-opacity duration-200',
        // 재조회 중에는 스켈레톤으로 깜빡이지 않고 이전 렌더를 흐리게 유지합니다.
        stale && 'opacity-60',
      )}
    >
      <figcaption className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id={captionId} className="text-[15px] font-semibold">
            최근 14일 매출
          </h2>
          <p className="mt-1 text-[13px] text-ink-soft">
            취소 건 제외 · 합계{' '}
            <b className="font-semibold text-ink">{formatPrice(total)}원</b>
          </p>
        </div>

        <div
          className="flex rounded-pill border border-line p-0.5"
          role="group"
          aria-label="보기 방식"
        >
          {[
            { id: 'chart', label: '차트', icon: BarChart3 },
            { id: 'table', label: '표', icon: Table2 },
          ].map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => setView(id)}
              aria-pressed={view === id}
              className={cn(
                'flex h-8 items-center gap-1.5 rounded-pill px-3 text-[12px] transition-colors',
                view === id ? 'bg-ink text-canvas' : 'text-ink-soft hover:text-ink',
              )}
            >
              <Icon size={13} aria-hidden="true" />
              {label}
            </button>
          ))}
        </div>
      </figcaption>

      {loading ? (
        <div className="hs-skeleton h-56 w-full rounded-sm" aria-label="매출 데이터를 불러오는 중" />
      ) : view === 'table' ? (
        <div className="max-h-64 overflow-y-auto">
          <table className="w-full text-[13px]">
            <caption className="sr-only">최근 14일 일별 매출과 주문 건수</caption>
            <thead className="sticky top-0 bg-surface">
              <tr className="border-b border-line text-left text-ink-soft">
                <th scope="col" className="py-2 font-medium">
                  날짜
                </th>
                <th scope="col" className="py-2 text-right font-medium">
                  매출
                </th>
                <th scope="col" className="py-2 text-right font-medium">
                  주문
                </th>
              </tr>
            </thead>
            <tbody>
              {data.map((d) => (
                <tr key={d.date} className="border-b border-line/60 last:border-0">
                  <td className="py-2">{formatShortDate(d.date)}</td>
                  <td className="tnum py-2 text-right">{formatPrice(d.revenue)}원</td>
                  <td className="tnum py-2 text-right text-ink-soft">{d.orders}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="relative">
          {/* 눈금 + 격자선 */}
          <div className="relative h-56 pl-14">
            {ticks
              .slice()
              .reverse()
              .map((tick) => (
                <div
                  key={tick}
                  className="absolute inset-x-0 left-14 flex items-center"
                  style={{ bottom: `${(tick / axisMax) * 100}%` }}
                >
                  <span className="tnum absolute -left-14 w-12 text-right text-[11px] text-ink-faint">
                    {tick === 0 ? '0' : formatCompactWon(tick)}
                  </span>
                  <span className="h-px w-full bg-line" aria-hidden="true" />
                </div>
              ))}

            {/* 막대 */}
            <div className="absolute inset-0 left-14 flex items-end gap-[2px]">
              {data.map((d, i) => {
                const heightPct = axisMax ? (d.revenue / axisMax) * 100 : 0;
                const isActive = active === i;
                return (
                  <button
                    key={d.date}
                    type="button"
                    onMouseEnter={() => setActive(i)}
                    onMouseLeave={() => setActive(null)}
                    onFocus={() => setActive(i)}
                    onBlur={() => setActive(null)}
                    aria-label={`${formatShortDate(d.date)} 매출 ${formatPrice(d.revenue)}원, 주문 ${d.orders}건`}
                    className="group relative flex h-full max-w-6 flex-1 cursor-default items-end justify-center rounded-sm outline-offset-2"
                  >
                    <span
                      className={cn(
                        'w-full rounded-t-[4px] bg-primary transition-[height,opacity] duration-300 ease-out-soft',
                        isActive ? 'opacity-100' : 'opacity-85 group-hover:opacity-100',
                      )}
                      style={{ height: `${Math.max(heightPct, d.revenue > 0 ? 1.5 : 0)}%` }}
                      aria-hidden="true"
                    />

                    {/* 값 라벨은 최고치 하루에만. 막대 높이를 기준으로 띄웁니다
                        (버튼 기준으로 놓으면 플롯 맨 위에 떠서 어느 막대의 값인지 알 수 없습니다) */}
                    {i === peakIndex && !isActive && (
                      <span
                        className="tnum pointer-events-none absolute left-1/2 mb-1 -translate-x-1/2 text-[11px] font-semibold whitespace-nowrap text-ink"
                        style={{ bottom: `${heightPct}%` }}
                        aria-hidden="true"
                      >
                        {formatCompactWon(d.revenue)}
                      </span>
                    )}

                    {isActive && (
                      <span
                        className="pointer-events-none absolute left-1/2 z-10 mb-2 -translate-x-1/2 rounded-md bg-ink px-2.5 py-1.5 text-canvas shadow-pop"
                        style={{ bottom: `${heightPct}%` }}
                        aria-hidden="true"
                      >
                        <span className="block text-[11px] whitespace-nowrap opacity-80">
                          {formatShortDate(d.date)}
                        </span>
                        <span className="tnum block text-[13px] font-semibold whitespace-nowrap">
                          {formatPrice(d.revenue)}원
                        </span>
                        <span className="tnum block text-[11px] whitespace-nowrap opacity-80">
                          주문 {d.orders}건
                        </span>
                      </span>
                    )}
                  </button>
                );
              })}
            </div>

            {/* baseline */}
            <span className="absolute inset-x-0 bottom-0 left-14 h-px bg-line-strong" aria-hidden="true" />
          </div>

          {/* x축 밴드 — 컨테이너 높이에 포함시켜 안쪽 스크롤이 생기지 않게 합니다 */}
          <div className="mt-2 flex gap-[2px] pl-14" aria-hidden="true">
            {data.map((d, i) => (
              <span
                key={d.date}
                className="tnum max-w-6 flex-1 text-center text-[10px] text-ink-faint"
              >
                {i % 2 === 0 ? d.date.slice(8) : ''}
              </span>
            ))}
          </div>
        </div>
      )}
    </figure>
  );
}
