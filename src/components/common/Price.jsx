import { cn, formatPrice } from '@/utils/format';

/**
 * 가격 표기.
 * 할인가가 있으면 정가는 취소선으로 함께 노출하되, 스크린리더에는
 * "정가 32,000원에서 27,200원으로 할인" 형태의 완결된 문장을 전달합니다.
 */
export default function Price({ price, salePrice, discountRate, size = 'md', className }) {
  const hasSale = Boolean(salePrice) && salePrice < price;
  const final = hasSale ? salePrice : price;

  const sizes = {
    sm: { main: 'text-[15px]', sub: 'text-[12px]', rate: 'text-[12px]' },
    md: { main: 'text-[17px]', sub: 'text-[13px]', rate: 'text-[13px]' },
    lg: { main: 'text-[26px]', sub: 'text-[15px]', rate: 'text-[16px]' },
  }[size];

  return (
    <p className={cn('flex flex-wrap items-baseline gap-x-2 gap-y-0.5', className)}>
      <span className="sr-only">
        {hasSale
          ? `정가 ${formatPrice(price)}원에서 ${discountRate}% 할인된 ${formatPrice(final)}원`
          : `${formatPrice(final)}원`}
      </span>

      {hasSale && (
        <span className={cn('tnum font-semibold text-sale', sizes.rate)} aria-hidden="true">
          {discountRate}%
        </span>
      )}
      <span className={cn('tnum font-semibold text-ink', sizes.main)} aria-hidden="true">
        {formatPrice(final)}
        <span className="ml-0.5 text-[0.75em] font-medium">원</span>
      </span>
      {hasSale && (
        <span className={cn('tnum text-ink-faint line-through', sizes.sub)} aria-hidden="true">
          {formatPrice(price)}
        </span>
      )}
    </p>
  );
}
