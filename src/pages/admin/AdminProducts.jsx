import { useMemo, useState } from 'react';
import { Check, PackageSearch, Search, X } from 'lucide-react';
import BeadArt from '@/components/common/BeadArt';
import Badge from '@/components/common/Badge';
import EmptyState from '@/components/common/EmptyState';
import { Skeleton } from '@/components/common/Skeleton';
import { useToast } from '@/components/common/Toast';
import { useGetAdminProductsQuery, useUpdateProductMutation } from '@/features/admin/adminApi';
import { useDebounce } from '@/hooks/useDebounce';
import { CATEGORIES } from '@/mocks/db';
import { cn, formatPrice } from '@/utils/format';

const categoryLabel = (id) => CATEGORIES.find((c) => c.id === id)?.label ?? id;

export default function AdminProducts() {
  const { toast } = useToast();
  const [keyword, setKeyword] = useState('');
  const debounced = useDebounce(keyword.trim(), 300);

  const queryArgs = useMemo(() => (debounced ? { q: debounced } : {}), [debounced]);
  const { data, isLoading, isFetching } = useGetAdminProductsQuery(queryArgs);
  const [updateProduct] = useUpdateProductMutation();

  const items = data?.items ?? [];

  const saveStock = async (product, stock) => {
    try {
      await updateProduct({ id: product.id, stock, queryArgs }).unwrap();
      toast(`${product.name} 재고를 ${stock}개로 저장했어요`);
    } catch (err) {
      toast(err?.data?.message ?? '재고를 저장하지 못했어요.', { tone: 'error' });
    }
  };

  return (
    <div className="mx-auto max-w-[1120px]">
      <header className="mb-5">
        <p className="eyebrow">Inventory</p>
        <h1 className="mt-2 text-[26px] leading-none font-semibold">상품·재고</h1>
        <p className="mt-2 text-[13px] text-ink-soft">
          재고가 적은 순으로 정렬됩니다. 여기서 바꾼 값은 쇼핑몰 화면에 바로 반영돼요.
        </p>
      </header>

      <div className="relative sm:w-72">
        <label htmlFor="product-search" className="sr-only">
          상품명 또는 상품코드 검색
        </label>
        <Search
          size={15}
          className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-ink-faint"
          aria-hidden="true"
        />
        <input
          id="product-search"
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="상품명 · 상품코드"
          className="h-9 w-full rounded-pill border border-line bg-surface pr-3 pl-9 text-[13px] outline-none focus:border-primary"
        />
      </div>

      <p className="tnum mt-3 text-[13px] text-ink-soft" aria-live="polite">
        {isLoading ? '불러오는 중…' : `${data?.total ?? 0}종`}
      </p>

      {isLoading ? (
        <div className="mt-3 space-y-2">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-md" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={PackageSearch}
          title="검색 결과가 없어요"
          description="상품명 일부나 상품코드(P0001)로 찾아보세요."
        />
      ) : (
        <ul
          className={cn(
            'mt-3 space-y-2 transition-opacity duration-200',
            isFetching && !isLoading && 'opacity-60',
          )}
        >
          {items.map((product) => (
            <ProductRow key={product.id} product={product} onSave={saveStock} />
          ))}
        </ul>
      )}
    </div>
  );
}

function ProductRow({ product, onSave }) {
  const [draft, setDraft] = useState(String(product.stock));
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  const parsed = Number(draft);
  const invalid = draft === '' || Number.isNaN(parsed) || parsed < 0 || parsed > 9999;
  const dirty = !invalid && parsed !== product.stock;

  const commit = async () => {
    if (!dirty) {
      setEditing(false);
      return;
    }
    setSaving(true);
    await onSave(product, parsed);
    setSaving(false);
    setEditing(false);
  };

  const cancel = () => {
    setDraft(String(product.stock));
    setEditing(false);
  };

  return (
    <li className="flex items-center gap-4 rounded-md border border-line bg-surface p-3 sm:p-4">
      <span className="media-frame size-14 shrink-0 rounded-sm">
        <BeadArt product={product} decorative className="h-full w-full" />
      </span>

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="tnum text-[11px] text-ink-faint">{product.id}</span>
          {product.stock === 0 && <Badge tone="soldout" />}
          {product.stock > 0 && product.stock <= 5 && <Badge tone="sale">재고 임박</Badge>}
        </div>
        <p className="mt-0.5 truncate text-[14px] font-medium">{product.name}</p>
        <p className="mt-0.5 text-[12px] text-ink-soft">
          {categoryLabel(product.category)} ·{' '}
          <span className="tnum">{formatPrice(product.salePrice ?? product.price)}원</span>
          {product.salePrice && (
            <span className="tnum text-sale"> ({product.discountRate}% 할인)</span>
          )}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        {editing ? (
          <>
            <div className="flex flex-col">
              <label htmlFor={`stock-${product.id}`} className="sr-only">
                {product.name} 재고 수량
              </label>
              <input
                id={`stock-${product.id}`}
                type="number"
                min={0}
                max={9999}
                value={draft}
                autoFocus
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') commit();
                  if (e.key === 'Escape') cancel();
                }}
                aria-invalid={invalid || undefined}
                aria-describedby={invalid ? `stock-err-${product.id}` : undefined}
                className={cn(
                  'tnum h-11 w-20 rounded-md border px-2.5 text-right text-[14px] outline-none',
                  invalid ? 'border-danger' : 'border-line focus:border-primary',
                )}
              />
            </div>

            <button
              type="button"
              onClick={commit}
              disabled={invalid || saving}
              aria-label="재고 저장"
              className="flex size-11 items-center justify-center rounded-full bg-primary text-on-primary transition-colors hover:bg-primary-hover disabled:bg-line disabled:text-ink-soft"
            >
              <Check size={17} aria-hidden="true" />
            </button>
            <button
              type="button"
              onClick={cancel}
              aria-label="수정 취소"
              className="flex size-11 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-canvas hover:text-ink"
            >
              <X size={17} aria-hidden="true" />
            </button>
          </>
        ) : (
          <button
            type="button"
            onClick={() => setEditing(true)}
            aria-label={`${product.name} 재고 ${product.stock}개, 수정하기`}
            className="flex h-11 items-center gap-2 rounded-pill border border-line px-4 transition-colors hover:border-ink"
          >
            <span className="text-[12px] text-ink-soft">재고</span>
            <span
              className={cn(
                'tnum text-[16px] font-semibold',
                product.stock === 0 ? 'text-danger' : product.stock <= 5 ? 'text-sale' : 'text-ink',
              )}
            >
              {product.stock}
            </span>
          </button>
        )}
      </div>

      {invalid && editing && (
        <p id={`stock-err-${product.id}`} role="alert" className="sr-only">
          재고는 0에서 9999 사이의 숫자여야 합니다.
        </p>
      )}
    </li>
  );
}
