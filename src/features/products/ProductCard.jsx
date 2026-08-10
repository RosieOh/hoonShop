import { memo } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Heart, ShoppingBag } from 'lucide-react';
import BeadArt from '@/components/common/BeadArt';
import Badge from '@/components/common/Badge';
import Price from '@/components/common/Price';
import Rating from '@/components/common/Rating';
import { useToast } from '@/components/common/Toast';
import { addToCart } from '@/features/cart/cartSlice';
import { toggleWish } from '@/features/cart/wishlistSlice';
import { cn } from '@/utils/format';

function ProductCard({ product, index = 0, priority = false }) {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const wished = useSelector((state) => state.wishlist.ids.includes(product.id));
  const soldOut = product.stock === 0;

  const handleQuickAdd = (e) => {
    e.preventDefault();
    dispatch(
      addToCart(product, {
        color: product.colorOptions[0].id,
        colorLabel: product.colorOptions[0].label,
        size: product.sizes[0] ?? null,
      }),
    );
    toast(`장바구니에 담았어요 · ${product.name}`, {
      action: { label: '보기', onClick: () => document.getElementById('cart-trigger')?.click() },
    });
  };

  const handleWish = (e) => {
    e.preventDefault();
    dispatch(toggleWish(product.id));
    toast(wished ? '위시리스트에서 뺐어요' : '위시리스트에 담았어요', { tone: 'info' });
  };

  return (
    <article
      className={cn('group hs-rise relative flex flex-col', soldOut && 'opacity-70')}
      // 그리드 진입 시 살짝 시차를 두면 한 덩어리로 튀어나오는 느낌이 사라집니다.
      style={{ animationDelay: priority ? '0ms' : `${Math.min(index, 11) * 45}ms` }}
    >
      <Link
        to={`/products/${product.id}`}
        className="media-frame block aspect-[4/5] rounded-sm"
        aria-label={`${product.name} 상세 보기`}
      >
        <BeadArt
          product={product}
          decorative
          className="h-full w-full transition-transform duration-500 ease-out-soft group-hover:scale-[1.045]"
        />

        <div className="pointer-events-none absolute top-2.5 left-2.5 flex flex-wrap gap-1">
          {product.discountRate > 0 && <Badge tone="sale">{product.discountRate}% OFF</Badge>}
          {product.badges.map((b) => (
            <Badge key={b} tone={b} />
          ))}
          {soldOut && <Badge tone="soldout" />}
        </div>

        {soldOut && (
          <div className="absolute inset-0 grid place-items-center bg-canvas/55">
            <span className="font-label rounded-pill bg-ink/85 px-4 py-2 text-[11px] font-semibold tracking-[0.16em] text-canvas">
              SOLD OUT
            </span>
          </div>
        )}
      </Link>

      {/* 빠른 담기 / 찜 — 데스크톱은 hover 노출, 터치 기기는 항상 노출 */}
      <div className="absolute top-2 right-2 flex flex-col gap-1.5 opacity-100 transition-opacity duration-200 md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100">
        <button
          type="button"
          onClick={handleWish}
          aria-pressed={wished}
          aria-label={`${product.name} 위시리스트 ${wished ? '해제' : '추가'}`}
          className="flex size-11 items-center justify-center rounded-full bg-surface/90 text-ink shadow-card backdrop-blur-sm transition-transform active:scale-90"
        >
          <Heart
            size={17}
            aria-hidden="true"
            className={cn(wished && 'text-sale')}
            fill={wished ? 'currentColor' : 'none'}
            strokeWidth={1.7}
          />
        </button>
        {!soldOut && (
          <button
            type="button"
            onClick={handleQuickAdd}
            aria-label={`${product.name} 장바구니에 바로 담기`}
            className="flex size-11 items-center justify-center rounded-full bg-surface/90 text-ink shadow-card backdrop-blur-sm transition-transform active:scale-90"
          >
            <ShoppingBag size={17} aria-hidden="true" strokeWidth={1.7} />
          </button>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-1 pt-3.5">
        <div className="flex items-center gap-2">
          <span className="eyebrow">{product.colorOptions[0].label}</span>
          {product.stock > 0 && product.stock <= 5 && (
            <span className="text-[11px] font-medium text-sale">{product.stock}개 남음</span>
          )}
        </div>

        <h3 className="text-[15px] leading-snug font-medium text-ink">
          <Link to={`/products/${product.id}`} className="hover:underline underline-offset-4">
            {product.name}
          </Link>
        </h3>

        <Price
          price={product.price}
          salePrice={product.salePrice}
          discountRate={product.discountRate}
          size="sm"
          className="mt-0.5"
        />

        <Rating value={product.rating} count={product.reviewCount} size={12} className="mt-1" />
      </div>
    </article>
  );
}

export default memo(ProductCard);
