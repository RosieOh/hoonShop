import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Trash2 } from 'lucide-react';
import BeadArt from '@/components/common/BeadArt';
import QuantityStepper from '@/components/common/QuantityStepper';
import { removeFromCart, toggleSelect, updateQuantity } from './cartSlice';
import { cn, formatPrice } from '@/utils/format';

/** 옵션 라벨: "민트 / 16.5cm (M)" */
function optionLabel(item) {
  return (
    [item.options.colorLabel ?? item.options.color, item.options.size].filter(Boolean).join(' / ') ||
    '기본 옵션'
  );
}

export default function CartItem({ item, selected, selectable = true, compact = false }) {
  const dispatch = useDispatch();
  const lineTotal = (item.salePrice ?? item.price) * item.quantity;
  const lowStock = item.stock > 0 && item.stock < item.quantity;

  return (
    <li className={cn('flex gap-3 py-4', compact ? 'px-0' : 'px-1')}>
      {selectable && (
        <div className="flex items-start pt-1">
          <input
            type="checkbox"
            checked={selected}
            onChange={() => dispatch(toggleSelect(item.lineId))}
            aria-label={`${item.name} 주문 대상으로 선택`}
            className="size-5 accent-[#7A3B52]"
          />
        </div>
      )}

      <Link
        to={`/products/${item.productId}`}
        className="media-frame size-22 shrink-0 rounded-sm sm:size-24"
        tabIndex={-1}
        aria-hidden="true"
      >
        <BeadArt product={item} decorative className="h-full w-full" />
      </Link>

      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="truncate text-[14px] font-medium">
              <Link to={`/products/${item.productId}`} className="hover:underline underline-offset-4">
                {item.name}
              </Link>
            </h3>
            <p className="mt-0.5 truncate text-[12px] text-ink-soft">{optionLabel(item)}</p>
          </div>

          <button
            type="button"
            onClick={() => dispatch(removeFromCart(item.lineId))}
            aria-label={`${item.name} 장바구니에서 삭제`}
            className="-mt-1.5 -mr-1.5 flex size-11 shrink-0 items-center justify-center rounded-full text-ink-faint transition-colors hover:bg-canvas hover:text-danger"
          >
            <Trash2 size={16} aria-hidden="true" />
          </button>
        </div>

        {item.stock === 0 && (
          <p className="mt-1.5 text-[12px] font-medium text-danger">품절된 상품입니다</p>
        )}
        {lowStock && (
          <p className="mt-1.5 text-[12px] font-medium text-sale">
            재고가 {item.stock}개 남아 수량을 줄여야 해요
          </p>
        )}

        <div className="mt-auto flex items-end justify-between gap-2 pt-2.5">
          <QuantityStepper
            value={item.quantity}
            onChange={(q) => dispatch(updateQuantity({ lineId: item.lineId, quantity: q }))}
            max={Math.max(1, Math.min(10, item.stock || 10))}
            size="sm"
            label={`${item.name} 수량`}
          />
          <p className="tnum shrink-0 text-[15px] font-semibold">
            {formatPrice(lineTotal)}
            <span className="ml-0.5 text-[12px] font-medium">원</span>
          </p>
        </div>
      </div>
    </li>
  );
}
