import ProductCard from './ProductCard';
import { ProductGridSkeleton } from '@/components/common/Skeleton';
import { cn } from '@/utils/format';

export default function ProductGrid({ products = [], loading, skeletonCount = 8, cols, className }) {
  if (loading && products.length === 0) return <ProductGridSkeleton count={skeletonCount} />;

  return (
    <div
      className={cn(
        'grid gap-x-4 gap-y-9 sm:gap-x-5',
        cols ?? 'grid-cols-2 md:grid-cols-3 lg:grid-cols-4',
        className,
      )}
    >
      {products.map((product, i) => (
        <ProductCard key={product.id} product={product} index={i} priority={i < 4} />
      ))}
    </div>
  );
}
