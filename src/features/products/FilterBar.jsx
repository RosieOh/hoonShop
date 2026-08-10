import { useState } from 'react';
import { Check, SlidersHorizontal, X } from 'lucide-react';
import Sheet from '@/components/common/Sheet';
import Button from '@/components/common/Button';
import { CATEGORIES, COLOR_SWATCHES, SORT_OPTIONS } from '@/mocks/db';
import { cn, formatPrice } from '@/utils/format';

const PRICE_RANGES = [
  { id: 'all', label: '전체', min: 0, max: undefined },
  { id: 'u20', label: '2만원 이하', min: 0, max: 20000 },
  { id: '20-30', label: '2~3만원', min: 20000, max: 30000 },
  { id: '30-40', label: '3~4만원', min: 30000, max: 40000 },
  { id: 'o40', label: '4만원 이상', min: 40000, max: undefined },
];

/**
 * 필터는 "지금 무엇이 걸려 있는지"가 항상 보여야 합니다.
 * 적용된 조건은 칩으로 노출하고, 칩 하나하나를 개별 해제할 수 있게 했습니다.
 */
export default function FilterBar({ filters, onChange, total }) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(filters);

  const openSheet = () => {
    setDraft(filters);
    setOpen(true);
  };

  const activeCount =
    (filters.colors?.length ?? 0) + (filters.priceRange && filters.priceRange !== 'all' ? 1 : 0);

  const toggleColor = (id) =>
    setDraft((d) => ({
      ...d,
      colors: d.colors.includes(id) ? d.colors.filter((c) => c !== id) : [...d.colors, id],
    }));

  const apply = () => {
    onChange(draft);
    setOpen(false);
  };

  const reset = () => setDraft({ ...draft, colors: [], priceRange: 'all' });

  return (
    <div className="sticky top-[var(--spacing-header)] z-20 -mx-4 bg-canvas/92 px-4 backdrop-blur-md">
      {/* 카테고리 — 가로 스크롤 (페이지 자체는 절대 가로 스크롤되지 않음) */}
      <div className="scrollbar-none -mx-4 overflow-x-auto px-4">
        <div className="flex w-max gap-2 py-3">
          {CATEGORIES.map((c) => {
            const selected = filters.category === c.id;
            return (
              <button
                key={c.id}
                type="button"
                onClick={() => onChange({ ...filters, category: c.id })}
                aria-pressed={selected}
                className={cn(
                  'h-10 rounded-pill border px-4 text-[14px] whitespace-nowrap transition-colors duration-150',
                  selected
                    ? 'border-ink bg-ink text-canvas'
                    : 'border-line bg-surface text-ink-soft hover:border-line-strong hover:text-ink',
                )}
              >
                {c.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 border-b border-line py-2.5">
        <p className="tnum text-[13px] text-ink-soft">
          총 <b className="font-semibold text-ink">{formatPrice(total ?? 0)}</b>개
        </p>

        <div className="flex items-center gap-1.5">
          <label className="sr-only" htmlFor="sort-select">
            정렬 기준
          </label>
          <select
            id="sort-select"
            value={filters.sort}
            onChange={(e) => onChange({ ...filters, sort: e.target.value })}
            className="h-10 rounded-pill border border-line bg-surface px-3 text-[13px] text-ink outline-none hover:border-line-strong"
          >
            {SORT_OPTIONS.map((s) => (
              <option key={s.id} value={s.id}>
                {s.label}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={openSheet}
            className={cn(
              'flex h-10 items-center gap-1.5 rounded-pill border px-3.5 text-[13px] transition-colors',
              activeCount
                ? 'border-primary bg-primary-soft text-primary'
                : 'border-line bg-surface text-ink hover:border-line-strong',
            )}
          >
            <SlidersHorizontal size={15} aria-hidden="true" />
            필터
            {activeCount > 0 && <span className="tnum font-semibold">{activeCount}</span>}
          </button>
        </div>
      </div>

      {activeCount > 0 && (
        <div className="flex flex-wrap gap-1.5 py-2.5">
          {filters.priceRange !== 'all' && (
            <Chip
              label={PRICE_RANGES.find((p) => p.id === filters.priceRange)?.label}
              onRemove={() => onChange({ ...filters, priceRange: 'all' })}
            />
          )}
          {filters.colors.map((id) => (
            <Chip
              key={id}
              label={COLOR_SWATCHES.find((c) => c.id === id)?.label}
              onRemove={() => onChange({ ...filters, colors: filters.colors.filter((c) => c !== id) })}
            />
          ))}
          <button
            type="button"
            onClick={() => onChange({ ...filters, colors: [], priceRange: 'all' })}
            className="h-8 px-2 text-[13px] text-ink-soft underline underline-offset-4 hover:text-ink"
          >
            전체 해제
          </button>
        </div>
      )}

      <Sheet
        open={open}
        onClose={() => setOpen(false)}
        title="필터"
        description="원하는 조건만 골라보세요"
        side="bottom"
        footer={
          <div className="flex gap-2">
            <Button variant="outline" onClick={reset} className="flex-1">
              초기화
            </Button>
            <Button onClick={apply} className="flex-[2]">
              적용하기
            </Button>
          </div>
        }
      >
        <div className="space-y-8 px-5 py-6">
          <fieldset>
            <legend className="eyebrow mb-3">가격대</legend>
            <div className="flex flex-wrap gap-2">
              {PRICE_RANGES.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => setDraft({ ...draft, priceRange: p.id })}
                  aria-pressed={draft.priceRange === p.id}
                  className={cn(
                    'h-11 rounded-pill border px-4 text-[14px] transition-colors',
                    draft.priceRange === p.id
                      ? 'border-primary bg-primary-soft font-medium text-primary'
                      : 'border-line text-ink-soft hover:border-line-strong',
                  )}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset>
            <legend className="eyebrow mb-3">컬러</legend>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {COLOR_SWATCHES.map((c) => {
                const selected = draft.colors.includes(c.id);
                return (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => toggleColor(c.id)}
                    aria-pressed={selected}
                    className={cn(
                      'flex h-12 items-center gap-2.5 rounded-md border px-3 text-left text-[14px] transition-colors',
                      selected ? 'border-primary bg-primary-soft' : 'border-line hover:border-line-strong',
                    )}
                  >
                    <span
                      className="size-6 shrink-0 rounded-full ring-1 ring-line-strong ring-inset"
                      style={{ background: c.hex }}
                      aria-hidden="true"
                    />
                    <span className="min-w-0 flex-1 truncate">{c.label}</span>
                    {selected && <Check size={15} className="shrink-0 text-primary" aria-hidden="true" />}
                  </button>
                );
              })}
            </div>
          </fieldset>
        </div>
      </Sheet>
    </div>
  );
}

function Chip({ label, onRemove }) {
  return (
    <span className="inline-flex h-8 items-center gap-1 rounded-pill bg-primary-soft pr-1 pl-3 text-[13px] text-primary">
      {label}
      <button
        type="button"
        onClick={onRemove}
        aria-label={`${label} 필터 해제`}
        className="flex size-6 items-center justify-center rounded-full hover:bg-primary/15"
      >
        <X size={13} aria-hidden="true" />
      </button>
    </span>
  );
}

export { PRICE_RANGES };
