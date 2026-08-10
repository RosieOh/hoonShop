import { useDispatch, useSelector } from 'react-redux';
import { ShoppingBag } from 'lucide-react';
import Sheet from '@/components/common/Sheet';
import Button from '@/components/common/Button';
import EmptyState from '@/components/common/EmptyState';
import CartItem from './CartItem';
import FreeShippingMeter from './FreeShippingMeter';
import { closeDrawer, selectCartSummary, selectSelectedIds } from './cartSlice';
import { formatPrice } from '@/utils/format';

export default function CartDrawer() {
  const dispatch = useDispatch();
  const open = useSelector((s) => s.cart.isDrawerOpen);
  const items = useSelector((s) => s.cart.items);
  const selected = useSelector(selectSelectedIds);
  const summary = useSelector(selectCartSummary);

  const close = () => dispatch(closeDrawer());

  return (
    <Sheet
      open={open}
      onClose={close}
      title="장바구니"
      description={items.length ? `${items.length}종 · ${summary.count}개 선택됨` : undefined}
      side="right"
      width="max-w-[420px]"
      footer={
        items.length > 0 && (
          <div className="space-y-3">
            <FreeShippingMeter summary={summary} />
            <div className="flex items-baseline justify-between">
              <span className="text-[14px] text-ink-soft">결제 예상 금액</span>
              <span className="tnum font-display text-[24px] font-semibold">
                {formatPrice(summary.payTotal + summary.shippingFee)}
                <span className="ml-0.5 text-[14px] font-medium">원</span>
              </span>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" to="/cart" onClick={close} className="flex-1">
                장바구니 보기
              </Button>
              <Button
                to="/checkout"
                onClick={close}
                className="flex-[1.4]"
                disabled={summary.count === 0}
              >
                주문하기
              </Button>
            </div>
          </div>
        )
      }
    >
      {items.length === 0 ? (
        <EmptyState
          icon={ShoppingBag}
          title="아직 담긴 상품이 없어요"
          description="마음에 드는 비즈 하나만 골라보세요."
          actionLabel="상품 둘러보기"
          actionTo="/products"
        />
      ) : (
        <ul className="divide-y divide-line px-5">
          {items.map((item) => (
            <CartItem key={item.lineId} item={item} selected={selected.includes(item.lineId)} compact />
          ))}
        </ul>
      )}
    </Sheet>
  );
}
