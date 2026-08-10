import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SearchX } from 'lucide-react';
import ProductGrid from '@/features/products/ProductGrid';
import FilterBar, { PRICE_RANGES } from '@/features/products/FilterBar';
import EmptyState from '@/components/common/EmptyState';
import Button from '@/components/common/Button';
import { ProductCardSkeleton } from '@/components/common/Skeleton';
import { useGetProductsQuery } from '@/features/products/productApi';
import { CATEGORIES } from '@/mocks/db';

const PAGE_SIZE = 12;

/**
 * 필터 상태는 URL 쿼리에 둡니다.
 * 그래야 뒤로가기·새로고침·링크 공유가 모두 자연스럽게 동작합니다.
 */
export default function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(1);
  const sentinelRef = useRef(null);

  const filters = useMemo(
    () => ({
      category: searchParams.get('category') ?? 'all',
      sort: searchParams.get('sort') ?? 'recommend',
      q: searchParams.get('q') ?? '',
      priceRange: searchParams.get('price') ?? 'all',
      colors: (searchParams.get('colors') ?? '').split(',').filter(Boolean),
    }),
    [searchParams],
  );

  // 조건이 바뀌면 첫 페이지부터 다시
  const colorKey = filters.colors.join(',');
  useEffect(() => {
    setPage(1);
  }, [filters.category, filters.sort, filters.q, filters.priceRange, colorKey]);

  const range = PRICE_RANGES.find((p) => p.id === filters.priceRange) ?? PRICE_RANGES[0];

  const { data, isLoading, isFetching } = useGetProductsQuery({
    category: filters.category,
    sort: filters.sort,
    q: filters.q || undefined,
    minPrice: range.min || undefined,
    maxPrice: range.max,
    colors: filters.colors.join(',') || undefined,
    page,
    size: PAGE_SIZE,
  });

  const items = data?.items ?? [];
  const hasNext = data?.hasNext ?? false;

  const handleChange = useCallback(
    (next) => {
      const params = {};
      if (next.category !== 'all') params.category = next.category;
      if (next.sort !== 'recommend') params.sort = next.sort;
      if (next.q) params.q = next.q;
      if (next.priceRange !== 'all') params.price = next.priceRange;
      if (next.colors.length) params.colors = next.colors.join(',');
      setSearchParams(params, { replace: true });
    },
    [setSearchParams],
  );

  // 무한 스크롤: 스크롤 이벤트 대신 IntersectionObserver (메인 스레드 부담이 적습니다)
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el || !hasNext || isFetching) return undefined;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) setPage((p) => p + 1);
      },
      { rootMargin: '600px 0px' },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasNext, isFetching]);

  const categoryLabel = CATEGORIES.find((c) => c.id === filters.category)?.label ?? '전체';
  const heading = filters.q ? `‘${filters.q}’ 검색 결과` : categoryLabel;

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-16 sm:px-6">
      <header className="pt-8 pb-1">
        <p className="eyebrow">{filters.q ? 'Search' : 'Collection'}</p>
        <h1 className="font-display mt-2 text-[34px] leading-none font-semibold sm:text-[42px]">
          {heading}
        </h1>
      </header>

      <FilterBar filters={filters} onChange={handleChange} total={data?.total} />

      <div className="mt-8">
        {!isLoading && items.length === 0 ? (
          <EmptyState
            icon={SearchX}
            title="조건에 맞는 상품이 없어요"
            description="필터를 조금 풀어보거나 다른 카테고리를 둘러보세요."
            actionLabel="필터 초기화"
            onAction={() => setSearchParams({}, { replace: true })}
          />
        ) : (
          <>
            <ProductGrid products={items} loading={isLoading} skeletonCount={PAGE_SIZE} />

            {/* 다음 페이지 로딩 자리를 미리 확보해 레이아웃이 밀리지 않게 합니다 */}
            {isFetching && page > 1 && (
              <div className="mt-9 grid grid-cols-2 gap-x-4 gap-y-9 md:grid-cols-3 lg:grid-cols-4">
                {Array.from({ length: 4 }, (_, i) => (
                  <ProductCardSkeleton key={i} />
                ))}
              </div>
            )}

            <div ref={sentinelRef} aria-hidden="true" className="h-px" />

            {/* 관찰자가 동작하지 않는 환경을 위한 대체 수단 */}
            {hasNext && !isFetching && (
              <div className="mt-12 flex justify-center">
                <Button variant="outline" onClick={() => setPage((p) => p + 1)}>
                  상품 더 보기
                </Button>
              </div>
            )}

            {!hasNext && items.length > 0 && (
              <p className="mt-14 text-center text-[13px] text-ink-faint">
                마지막 상품까지 모두 보셨어요
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
