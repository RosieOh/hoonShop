import { NavLink, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Heart, Home, LayoutGrid, ShoppingBag, User } from 'lucide-react';
import { selectCartCount } from '@/features/cart/cartSlice';
import { cn } from '@/utils/format';

/**
 * 모바일 하단 내비게이션.
 * 항목은 5개를 넘기지 않습니다 — 그 이상은 탭 폭이 좁아져 오터치가 급증합니다.
 * 결제 흐름(체크아웃)에서는 이탈을 막기 위해 숨깁니다.
 */
export default function BottomNav() {
  const location = useLocation();
  const cartCount = useSelector(selectCartCount);
  const wishCount = useSelector((s) => s.wishlist.ids.length);

  if (location.pathname.startsWith('/checkout')) return null;

  const itemClass = ({ isActive }) =>
    cn(
      'relative flex h-full min-w-16 flex-1 flex-col items-center justify-center gap-1 text-[11px] transition-colors',
      isActive ? 'text-primary' : 'text-ink-soft',
    );

  return (
    <nav
      aria-label="주요 메뉴"
      className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-canvas/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-md sm:hidden"
    >
      <div className="flex h-16">
        <NavLink to="/" end className={itemClass}>
          <Home size={20} strokeWidth={1.6} aria-hidden="true" />
          홈
        </NavLink>

        <NavLink to="/products" className={itemClass}>
          <LayoutGrid size={20} strokeWidth={1.6} aria-hidden="true" />
          전체상품
        </NavLink>

        <NavLink to="/wishlist" className={itemClass}>
          <span className="relative">
            <Heart size={20} strokeWidth={1.6} aria-hidden="true" />
            {wishCount > 0 && <Dot />}
          </span>
          위시
        </NavLink>

        <NavLink to="/cart" className={itemClass}>
          <span className="relative">
            <ShoppingBag size={20} strokeWidth={1.6} aria-hidden="true" />
            {cartCount > 0 && (
              <span
                className="tnum absolute -top-1.5 -right-2.5 min-w-[17px] rounded-pill bg-primary px-1 py-0.5 text-[10px] leading-none font-semibold text-on-primary"
                aria-hidden="true"
              >
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </span>
          장바구니
        </NavLink>

        <NavLink to="/mypage" className={itemClass}>
          <User size={20} strokeWidth={1.6} aria-hidden="true" />
          마이
        </NavLink>
      </div>
    </nav>
  );
}

function Dot() {
  return (
    <span
      className="absolute -top-0.5 -right-1 size-1.5 rounded-full bg-primary"
      aria-hidden="true"
    />
  );
}
