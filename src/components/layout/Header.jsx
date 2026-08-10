import { useEffect, useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Heart, Search, ShoppingBag, User } from 'lucide-react';
import { IconButton } from '@/components/common/Button';
import EventBanner from '@/features/marketing/EventBanner';
import { openDrawer, selectCartCount } from '@/features/cart/cartSlice';
import { openSearch } from '@/features/search/searchSlice';
import { CATEGORIES } from '@/mocks/db';
import { cn } from '@/utils/format';

const NAV = CATEGORIES.filter((c) => c.id !== 'all');

export default function Header() {
  const dispatch = useDispatch();
  const cartCount = useSelector(selectCartCount);
  const wishCount = useSelector((s) => s.wishlist.ids.length);
  const isAuthed = useSelector((s) => s.auth.isAuthenticated);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <>
      <a href="#main" className="sr-only-focusable">
        본문으로 건너뛰기
      </a>

      <EventBanner />

      <header
        className={cn(
          'sticky top-0 z-30 bg-canvas/88 backdrop-blur-md transition-shadow duration-200',
          scrolled ? 'shadow-[0_1px_0_var(--color-line)]' : 'shadow-none',
        )}
      >
        <div className="mx-auto flex h-[var(--spacing-header)] max-w-[1240px] items-center gap-3 px-4 sm:px-6">
          <Link
            to="/"
            className="font-display text-[26px] leading-none font-semibold tracking-[-0.02em] text-ink"
            aria-label="hoonshop 홈으로"
          >
            hoon<span className="text-primary">shop</span>
          </Link>

          <nav aria-label="카테고리" className="ml-6 hidden lg:block">
            <ul className="flex items-center gap-1">
              {NAV.map((c) => (
                <li key={c.id}>
                  <NavLink
                    to={`/products?category=${c.id}`}
                    className={({ isActive }) =>
                      cn(
                        'flex h-11 items-center rounded-pill px-3.5 text-[14px] transition-colors',
                        isActive ? 'text-primary' : 'text-ink-soft hover:bg-primary-soft hover:text-ink',
                      )
                    }
                  >
                    {c.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>

          <div className="ml-auto flex items-center gap-0.5">
            <IconButton label="검색 열기" icon={Search} onClick={() => dispatch(openSearch())} />
            <Link
              to="/wishlist"
              aria-label={`위시리스트, ${wishCount}개 담김`}
              className="relative hidden size-11 items-center justify-center rounded-full text-ink transition-colors hover:bg-primary-soft sm:inline-flex"
            >
              <Heart size={20} strokeWidth={1.6} aria-hidden="true" />
              {wishCount > 0 && (
                <span
                  className="tnum absolute top-1 right-0.5 min-w-[19px] rounded-pill bg-primary px-1 py-0.5 text-[11px] leading-none font-semibold text-on-primary"
                  aria-hidden="true"
                >
                  {wishCount > 99 ? '99+' : wishCount}
                </span>
              )}
            </Link>
            <Link
              to="/mypage"
              aria-label={isAuthed ? '마이페이지' : '로그인'}
              className="hidden size-11 items-center justify-center rounded-full text-ink transition-colors hover:bg-primary-soft sm:inline-flex"
            >
              <User size={20} strokeWidth={1.6} aria-hidden="true" />
            </Link>
            <IconButton
              id="cart-trigger"
              label={`장바구니 (${cartCount}개)`}
              icon={ShoppingBag}
              badge={cartCount}
              onClick={() => dispatch(openDrawer())}
            />
          </div>
        </div>
      </header>
    </>
  );
}
