import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ExternalLink,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageCircleQuestion,
  Package,
  Receipt,
  X,
} from 'lucide-react';
import { useToast } from '@/components/common/Toast';
import { logout } from '@/features/auth/authSlice';
import { useGetStatsQuery } from '@/features/admin/adminApi';
import { cn } from '@/utils/format';

const NAV = [
  { to: '/admin', end: true, label: '대시보드', icon: LayoutDashboard },
  { to: '/admin/orders', label: '주문 관리', icon: Receipt, badge: 'needsAction' },
  { to: '/admin/products', label: '상품·재고', icon: Package, badge: 'lowStockCount' },
  { to: '/admin/inquiries', label: '문의', icon: MessageCircleQuestion, badge: 'unansweredQna' },
];

/**
 * 관리자 셸.
 *
 * 스토어프론트와 의도적으로 다른 밀도를 씁니다 — 쇼핑 화면은 여백으로 상품을 돋보이게 하지만,
 * 운영 화면은 한 화면에 얼마나 담기느냐가 곧 업무 속도입니다.
 * 대신 토큰(색·서체)은 공유해서 같은 브랜드로 읽히게 했습니다.
 */
export default function AdminLayout() {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const user = useSelector((s) => s.auth.user);
  const { data: stats } = useGetStatsQuery();
  const [navOpen, setNavOpen] = useState(false);

  const sidebar = (
    <nav aria-label="관리 메뉴" className="flex h-full flex-col">
      <div className="flex h-14 items-center gap-2 px-5">
        <Link to="/admin" className="font-display text-[20px] leading-none font-semibold">
          hoon<span className="text-primary">shop</span>
        </Link>
        <span className="font-label rounded-sm bg-ink px-1.5 py-1 text-[9px] font-semibold tracking-[0.14em] text-canvas">
          ADMIN
        </span>
      </div>

      <ul className="mt-2 flex-1 space-y-0.5 px-3">
        {NAV.map(({ to, end, label, icon: Icon, badge }) => {
          const count = badge ? (stats?.[badge] ?? 0) : 0;
          return (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                onClick={() => setNavOpen(false)}
                className={({ isActive }) =>
                  cn(
                    'flex h-11 items-center gap-2.5 rounded-md px-3 text-[14px] transition-colors',
                    isActive
                      ? 'bg-primary-soft font-medium text-primary'
                      : 'text-ink-soft hover:bg-canvas hover:text-ink',
                  )
                }
              >
                <Icon size={17} strokeWidth={1.7} aria-hidden="true" />
                <span className="flex-1">{label}</span>
                {count > 0 && (
                  <span
                    className="tnum rounded-pill bg-primary px-1.5 py-0.5 text-[11px] leading-none font-semibold text-on-primary"
                    aria-label={`${count}건 처리 필요`}
                  >
                    {count}
                  </span>
                )}
              </NavLink>
            </li>
          );
        })}
      </ul>

      <div className="border-t border-line p-3">
        <Link
          to="/"
          className="flex h-11 items-center gap-2.5 rounded-md px-3 text-[13px] text-ink-soft transition-colors hover:bg-canvas hover:text-ink"
        >
          <ExternalLink size={16} strokeWidth={1.7} aria-hidden="true" />
          쇼핑몰 화면 보기
        </Link>
        <button
          type="button"
          onClick={() => {
            dispatch(logout());
            toast('로그아웃되었어요', { tone: 'info' });
          }}
          className="flex h-11 w-full items-center gap-2.5 rounded-md px-3 text-[13px] text-ink-soft transition-colors hover:bg-canvas hover:text-ink"
        >
          <LogOut size={16} strokeWidth={1.7} aria-hidden="true" />
          로그아웃
        </button>
      </div>
    </nav>
  );

  return (
    <div className="flex min-h-dvh bg-canvas">
      <a href="#admin-main" className="sr-only-focusable">
        본문으로 건너뛰기
      </a>

      {/* 데스크톱 고정 사이드바 */}
      <aside className="sticky top-0 hidden h-dvh w-60 shrink-0 border-r border-line bg-surface lg:block">
        {sidebar}
      </aside>

      {/* 모바일 드로어 */}
      {navOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            aria-label="메뉴 닫기"
            onClick={() => setNavOpen(false)}
            className="hs-fade absolute inset-0 h-full w-full cursor-default bg-ink/35"
          />
          <div className="hs-slide-left absolute top-0 right-0 h-full w-64 bg-surface shadow-pop">
            {sidebar}
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-canvas/90 px-4 backdrop-blur-md sm:px-6">
          <Link to="/admin" className="font-display text-[18px] font-semibold lg:hidden">
            hoon<span className="text-primary">shop</span>
          </Link>

          <p className="ml-auto hidden text-[13px] text-ink-soft sm:block">
            {user?.name}님 · <span className="text-ink">운영자</span>
          </p>

          <button
            type="button"
            onClick={() => setNavOpen((v) => !v)}
            aria-label={navOpen ? '관리 메뉴 닫기' : '관리 메뉴 열기'}
            aria-expanded={navOpen}
            className="-mr-2 flex size-11 items-center justify-center rounded-full text-ink transition-colors hover:bg-primary-soft lg:hidden"
          >
            {navOpen ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
          </button>
        </header>

        <main id="admin-main" className="min-w-0 flex-1 px-4 py-6 sm:px-6 sm:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
