import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ChevronRight, Heart, PackageX, Truck } from 'lucide-react';
import BeadArt, { paletteFromHex } from '@/components/common/BeadArt';
import Badge from '@/components/common/Badge';
import Button from '@/components/common/Button';
import Price from '@/components/common/Price';
import Rating from '@/components/common/Rating';
import QuantityStepper from '@/components/common/QuantityStepper';
import EmptyState from '@/components/common/EmptyState';
import { Skeleton } from '@/components/common/Skeleton';
import { useToast } from '@/components/common/Toast';
import ProductGrid from '@/features/products/ProductGrid';
import ReviewSection from '@/features/cs/ReviewSection';
import QnaList from '@/features/cs/QnaList';
import { useGetProductDetailQuery } from '@/features/products/productApi';
import { addToCart, openDrawer } from '@/features/cart/cartSlice';
import { toggleWish } from '@/features/cart/wishlistSlice';
import { cn, formatPrice } from '@/utils/format';

const TABS = [
  { id: 'detail', label: '상품 설명' },
  { id: 'review', label: '리뷰' },
  { id: 'qna', label: '문의' },
  { id: 'shipping', label: '배송·교환' },
];

export default function ProductDetailPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { toast } = useToast();

  const { data: product, isLoading, isError } = useGetProductDetailQuery(id);
  const wished = useSelector((s) => s.wishlist.ids.includes(id));

  const [color, setColor] = useState(null);
  const [size, setSize] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [tab, setTab] = useState('detail');
  const [showOptionError, setShowOptionError] = useState(false);

  // 상품이 바뀌면 선택 상태를 초기화합니다.
  useEffect(() => {
    setColor(null);
    setSize(null);
    setQuantity(1);
    setTab('detail');
    setShowOptionError(false);
  }, [id]);

  const needsSize = (product?.sizes?.length ?? 0) > 0;
  const optionsReady = Boolean(color) && (!needsSize || Boolean(size));
  const soldOut = product?.stock === 0;

  const selectedColor = useMemo(
    () => product?.colorOptions?.find((c) => c.id === color),
    [product, color],
  );

  const previewProduct = useMemo(
    () => (product && selectedColor ? { ...product, palette: paletteFromHex(selectedColor.hex) } : product),
    [product, selectedColor],
  );

  if (isLoading) return <DetailSkeleton />;

  if (isError || !product) {
    return (
      <EmptyState
        icon={PackageX}
        title="상품을 찾을 수 없어요"
        description="주소가 바뀌었거나 판매가 종료된 상품일 수 있습니다."
        actionLabel="전체 상품 보기"
        actionTo="/products"
      />
    );
  }

  const handleAdd = (goToCheckout = false) => {
    if (!optionsReady) {
      setShowOptionError(true);
      document.getElementById('option-picker')?.scrollIntoView({ block: 'center' });
      return;
    }

    dispatch(
      addToCart(
        product,
        { color: selectedColor.id, colorLabel: selectedColor.label, size },
        quantity,
      ),
    );

    if (goToCheckout) {
      navigate('/checkout');
    } else {
      dispatch(openDrawer());
      toast('장바구니에 담았어요');
    }
  };

  const unitPrice = product.salePrice ?? product.price;

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-28 sm:px-6 sm:pb-16">
      <nav aria-label="현재 위치" className="flex items-center gap-1 py-4 text-[12px] text-ink-soft">
        <Link to="/" className="hover:text-ink">
          홈
        </Link>
        <ChevronRight size={13} aria-hidden="true" />
        <Link to={`/products?category=${product.category}`} className="hover:text-ink">
          {product.colorOptions[0].label} 계열
        </Link>
        <ChevronRight size={13} aria-hidden="true" />
        <span className="truncate text-ink">{product.name}</span>
      </nav>

      <div className="grid gap-10 lg:grid-cols-2 lg:gap-14">
        {/* -------------------------------------------------------- 이미지 --- */}
        <div className="lg:sticky lg:top-24 lg:self-start">
          <div className="media-frame aspect-square rounded-md">
            <BeadArt product={previewProduct} className="h-full w-full" />
            <div className="absolute top-3 left-3 flex gap-1">
              {product.discountRate > 0 && <Badge tone="sale">{product.discountRate}% OFF</Badge>}
              {product.badges.map((b) => (
                <Badge key={b} tone={b} />
              ))}
            </div>
          </div>

          <ul className="mt-3 flex gap-2" aria-label="컬러별 미리보기">
            {product.colorOptions.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => setColor(c.id)}
                  aria-pressed={color === c.id}
                  aria-label={`${c.label} 색상 미리보기`}
                  className={cn(
                    'media-frame size-18 rounded-sm ring-2 transition-[box-shadow]',
                    color === c.id ? 'ring-primary' : 'ring-transparent hover:ring-line-strong',
                  )}
                >
                  <BeadArt
                    product={{ ...product, palette: paletteFromHex(c.hex) }}
                    decorative
                    className="h-full w-full"
                  />
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* ---------------------------------------------------------- 정보 --- */}
        <div>
          <p className="eyebrow">{product.materials[0]}</p>
          <h1 className="font-display mt-2.5 text-[36px] leading-tight font-semibold sm:text-[44px]">
            {product.name}
          </h1>

          <div className="mt-3 flex items-center gap-3">
            <Rating value={product.rating} count={product.reviewCount} />
            <button
              type="button"
              onClick={() => setTab('review')}
              className="text-[13px] text-ink-soft underline underline-offset-4 hover:text-ink"
            >
              리뷰 보기
            </button>
          </div>

          <Price
            price={product.price}
            salePrice={product.salePrice}
            discountRate={product.discountRate}
            size="lg"
            className="mt-6"
          />

          <p className="mt-5 text-[15px] leading-relaxed text-ink-soft">{product.description}</p>

          <dl className="mt-6 space-y-2 border-y border-line py-5 text-[13px]">
            <div className="flex gap-3">
              <dt className="w-20 shrink-0 text-ink-soft">소재</dt>
              <dd>{product.materials.join(' · ')}</dd>
            </div>
            <div className="flex gap-3">
              <dt className="w-20 shrink-0 text-ink-soft">배송</dt>
              <dd>
                5만원 이상 무료 · 그 미만 3,000원
                {product.stock > 0 && product.stock <= 5 && (
                  <span className="ml-2 font-medium text-sale">재고 {product.stock}개</span>
                )}
              </dd>
            </div>
            <div className="flex gap-3">
              <dt className="w-20 shrink-0 text-ink-soft">제작 기간</dt>
              <dd>결제 후 2~3 영업일</dd>
            </div>
          </dl>

          {/* ------------------------------------------------------ 옵션 --- */}
          <div id="option-picker" className="mt-7 space-y-6">
            <fieldset>
              <legend className="mb-2.5 flex items-center gap-2 text-[14px] font-medium">
                컬러
                {selectedColor && <span className="text-ink-soft">· {selectedColor.label}</span>}
              </legend>
              <div className="flex flex-wrap gap-2">
                {product.colorOptions.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => {
                      setColor(c.id);
                      setShowOptionError(false);
                    }}
                    aria-pressed={color === c.id}
                    className={cn(
                      'flex h-12 items-center gap-2 rounded-pill border pr-4 pl-2 text-[14px] transition-colors',
                      color === c.id
                        ? 'border-primary bg-primary-soft font-medium text-primary'
                        : 'border-line hover:border-line-strong',
                    )}
                  >
                    <span
                      className="size-8 rounded-full ring-1 ring-line-strong ring-inset"
                      style={{ background: c.hex }}
                      aria-hidden="true"
                    />
                    {c.label}
                  </button>
                ))}
              </div>
            </fieldset>

            {needsSize && (
              <fieldset>
                <legend className="mb-2.5 text-[14px] font-medium">사이즈</legend>
                <div className="flex flex-wrap gap-2">
                  {product.sizes.map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => {
                        setSize(s);
                        setShowOptionError(false);
                      }}
                      aria-pressed={size === s}
                      className={cn(
                        'h-12 rounded-pill border px-4 text-[14px] transition-colors',
                        size === s
                          ? 'border-primary bg-primary-soft font-medium text-primary'
                          : 'border-line hover:border-line-strong',
                      )}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </fieldset>
            )}

            <div className="flex items-center justify-between gap-4">
              <span className="text-[14px] font-medium">수량</span>
              <QuantityStepper
                value={quantity}
                onChange={setQuantity}
                max={Math.max(1, Math.min(10, product.stock || 10))}
              />
            </div>

            {showOptionError && (
              <p role="alert" className="rounded-md bg-[#FDF2F1] px-4 py-3 text-[13px] text-danger">
                {!color ? '컬러를 먼저 선택해 주세요.' : '사이즈를 선택해 주세요.'}
              </p>
            )}

            <div className="flex items-center justify-between border-t border-line pt-5">
              <span className="text-[14px] text-ink-soft">총 상품 금액</span>
              <span className="tnum font-display text-[30px] leading-none font-semibold">
                {formatPrice(unitPrice * quantity)}
                <span className="ml-1 text-[16px] font-medium">원</span>
              </span>
            </div>
          </div>

          {/* 데스크톱 CTA (모바일은 하단 고정 바) */}
          <div className="mt-6 hidden gap-2 sm:flex">
            <button
              type="button"
              onClick={() => dispatch(toggleWish(product.id))}
              aria-pressed={wished}
              aria-label={`위시리스트 ${wished ? '해제' : '추가'}`}
              className="flex size-14 shrink-0 items-center justify-center rounded-pill border border-line-strong transition-colors hover:border-ink"
            >
              <Heart
                size={20}
                className={cn(wished && 'text-sale')}
                fill={wished ? 'currentColor' : 'none'}
                strokeWidth={1.6}
                aria-hidden="true"
              />
            </button>
            <Button variant="outline" size="lg" className="flex-1" onClick={() => handleAdd(false)} disabled={soldOut}>
              장바구니
            </Button>
            <Button size="lg" className="flex-1" onClick={() => handleAdd(true)} disabled={soldOut}>
              {soldOut ? '품절' : '바로 구매'}
            </Button>
          </div>

          <p className="mt-4 flex items-center gap-1.5 text-[12px] text-ink-soft">
            <Truck size={13} aria-hidden="true" />
            평일 14시 이전 결제 시 당일 제작 시작
          </p>
        </div>
      </div>

      {/* ---------------------------------------------------------- 탭 --- */}
      <div className="mt-20">
        <div role="tablist" aria-label="상품 정보" className="flex gap-1 border-b border-line">
          {TABS.map((t) => (
            <button
              key={t.id}
              role="tab"
              id={`tab-${t.id}`}
              aria-selected={tab === t.id}
              aria-controls={`panel-${t.id}`}
              onClick={() => setTab(t.id)}
              className={cn(
                'relative h-13 flex-1 text-[14px] transition-colors sm:flex-none sm:px-8',
                tab === t.id ? 'font-semibold text-ink' : 'text-ink-soft hover:text-ink',
              )}
            >
              {t.label}
              {tab === t.id && (
                <span className="absolute inset-x-0 -bottom-px h-0.5 bg-ink" aria-hidden="true" />
              )}
            </button>
          ))}
        </div>

        <div id={`panel-${tab}`} role="tabpanel" aria-labelledby={`tab-${tab}`} tabIndex={0} className="pt-8 outline-none">
          {tab === 'detail' && (
            <div className="hs-fade max-w-2xl space-y-6 text-[15px] leading-relaxed text-ink-soft">
              <p>{product.description}</p>
              <div className="media-frame aspect-[16/10] rounded-md">
                <BeadArt product={previewProduct} decorative className="h-full w-full" />
              </div>
              <ul className="space-y-2">
                {product.materials.map((m) => (
                  <li key={m} className="flex gap-2">
                    <span className="mt-2 size-1 shrink-0 rounded-full bg-accent" aria-hidden="true" />
                    {m}
                  </li>
                ))}
              </ul>
              <p className="rounded-md bg-canvas p-5 text-[14px]">
                수공예 특성상 비즈의 결과 색이 미세하게 다를 수 있습니다. 이는 불량이 아닌 개별
                제품의 고유한 성질입니다.
              </p>
            </div>
          )}
          {tab === 'review' && <ReviewSection productId={product.id} />}
          {tab === 'qna' && <QnaList productId={product.id} />}
          {tab === 'shipping' && (
            <div className="hs-fade max-w-2xl space-y-5 text-[14px] leading-relaxed text-ink-soft">
              <section>
                <h3 className="text-[15px] font-semibold text-ink">배송</h3>
                <p className="mt-1.5">
                  결제 확인 후 2~3 영업일 내 제작·발송됩니다. 5만원 이상 구매 시 배송비가 무료이며,
                  미만은 3,000원이 부과됩니다.
                </p>
              </section>
              <section>
                <h3 className="text-[15px] font-semibold text-ink">교환·반품</h3>
                <p className="mt-1.5">
                  수령 후 7일 이내, 미착용 상태에서 가능합니다. 단순 변심의 경우 왕복 배송비
                  6,000원이 부과됩니다. 주문 제작 특성상 컬러를 별도 요청한 상품은 교환이
                  어렵습니다.
                </p>
              </section>
            </div>
          )}
        </div>
      </div>

      {/* -------------------------------------------------------- 연관 --- */}
      {product.related?.length > 0 && (
        <section aria-labelledby="related-heading" className="mt-20">
          <h2 id="related-heading" className="font-display text-[26px] font-semibold">
            함께 보면 좋은 것
          </h2>
          <div className="mt-6">
            <ProductGrid products={product.related} />
          </div>
        </section>
      )}

      {/* ------------------------------------------------ 모바일 고정 CTA --- */}
      <div className="fixed inset-x-0 bottom-16 z-20 flex gap-2 border-t border-line bg-canvas/95 px-4 py-3 backdrop-blur-md sm:hidden">
        <button
          type="button"
          onClick={() => dispatch(toggleWish(product.id))}
          aria-pressed={wished}
          aria-label={`위시리스트 ${wished ? '해제' : '추가'}`}
          className="flex size-12 shrink-0 items-center justify-center rounded-pill border border-line-strong"
        >
          <Heart
            size={19}
            className={cn(wished && 'text-sale')}
            fill={wished ? 'currentColor' : 'none'}
            strokeWidth={1.6}
            aria-hidden="true"
          />
        </button>
        <Button variant="outline" className="flex-1" onClick={() => handleAdd(false)} disabled={soldOut}>
          장바구니
        </Button>
        <Button className="flex-1" onClick={() => handleAdd(true)} disabled={soldOut}>
          {soldOut ? '품절' : '바로 구매'}
        </Button>
      </div>
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="mx-auto grid max-w-[1240px] gap-10 px-4 py-10 sm:px-6 lg:grid-cols-2">
      <Skeleton className="aspect-square w-full rounded-md" />
      <div className="space-y-4">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="h-10 w-4/5" />
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-14 w-full rounded-pill" />
      </div>
    </div>
  );
}
