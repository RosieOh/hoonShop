import { useSelector } from 'react-redux';
import { HeartOff } from 'lucide-react';
import EmptyState from '@/components/common/EmptyState';
import ProductGrid from '@/features/products/ProductGrid';
import { useGetProductsQuery } from '@/features/products/productApi';

export default function WishlistPage() {
  const ids = useSelector((s) => s.wishlist.ids);
  // 전체 목록을 캐시에서 가져와 로컬 위시 id로 걸러냅니다.
  // (서버 연동 시 GET /wishlist 로 교체)
  const { data, isLoading } = useGetProductsQuery({ size: 100 });

  const items = (data?.items ?? []).filter((p) => ids.includes(p.id));

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-16 sm:px-6">
      <header className="pt-8 pb-8">
        <p className="eyebrow">Wishlist</p>
        <h1 className="font-display mt-2 text-[34px] leading-none font-semibold sm:text-[42px]">
          찜한 상품
        </h1>
        {ids.length > 0 && (
          <p className="tnum mt-3 text-[14px] text-ink-soft">{ids.length}개를 담아두셨어요</p>
        )}
      </header>

      {!isLoading && items.length === 0 ? (
        <EmptyState
          icon={HeartOff}
          title="아직 찜한 상품이 없어요"
          description="상품 카드의 하트를 눌러 마음에 드는 것을 모아두세요."
          actionLabel="상품 둘러보기"
          actionTo="/products"
        />
      ) : (
        <ProductGrid products={items} loading={isLoading} skeletonCount={4} />
      )}
    </div>
  );
}
