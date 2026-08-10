import { cn } from '@/utils/format';

/** 빈 화면 대신 최종 레이아웃과 같은 크기의 골격을 먼저 보여줍니다 (CLS 방지). */
export function Skeleton({ className }) {
  return <div className={cn('hs-skeleton rounded-sm', className)} aria-hidden="true" />;
}

export function ProductCardSkeleton() {
  return (
    <div className="flex flex-col gap-3">
      <Skeleton className="aspect-[4/5] w-full" />
      <Skeleton className="h-3.5 w-1/3" />
      <Skeleton className="h-4 w-4/5" />
      <Skeleton className="h-4 w-1/2" />
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }) {
  return (
    <div
      className="grid grid-cols-2 gap-x-4 gap-y-9 md:grid-cols-3 lg:grid-cols-4"
      role="status"
      aria-label="상품을 불러오는 중"
    >
      {Array.from({ length: count }, (_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
}
