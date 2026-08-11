import { Outlet } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import BottomNav from './BottomNav';
import CartDrawer from '@/features/cart/CartDrawer';
import SearchOverlay from '@/features/search/SearchOverlay';

/** 쇼핑몰(고객) 셸. 관리자 구간은 AdminLayout이 따로 감쌉니다. */
export default function StoreLayout() {
  return (
    <div className="flex min-h-dvh flex-col">
      <Header />

      <main id="main" className="flex-1 pb-16 sm:pb-0">
        <Outlet />
      </main>

      <Footer />
      <BottomNav />

      {/* 전역 오버레이 — 고객 화면 어디서든 열립니다 */}
      <CartDrawer />
      <SearchOverlay />
    </div>
  );
}
