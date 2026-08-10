import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';
import BottomNav from '@/components/layout/BottomNav';
import ScrollToTop from '@/components/layout/ScrollToTop';
import CartDrawer from '@/features/cart/CartDrawer';
import SearchOverlay from '@/features/search/SearchOverlay';
import { ProductGridSkeleton } from '@/components/common/Skeleton';
import Home from '@/pages/Home';
import ProductListPage from '@/pages/ProductListPage';
import ProductDetailPage from '@/pages/ProductDetailPage';
import CartPage from '@/pages/CartPage';
import NotFoundPage from '@/pages/NotFoundPage';

// 결제 흐름은 첫 진입에 필요 없으므로 분리해 초기 번들을 가볍게 유지합니다.
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'));
const OrderCompletePage = lazy(() => import('@/pages/OrderCompletePage'));
const WishlistPage = lazy(() => import('@/pages/WishlistPage'));
const MyPage = lazy(() => import('@/pages/MyPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));

export default function App() {
  return (
    <div className="flex min-h-dvh flex-col">
      <ScrollToTop />
      <Header />

      <main id="main" className="flex-1 pb-16 sm:pb-0">
        <Suspense
          fallback={
            <div className="mx-auto max-w-[1240px] px-4 py-12 sm:px-6">
              <ProductGridSkeleton count={4} />
            </div>
          }
        >
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/products" element={<ProductListPage />} />
            <Route path="/products/:id" element={<ProductDetailPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/orders/complete" element={<OrderCompletePage />} />
            <Route path="/wishlist" element={<WishlistPage />} />
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </main>

      <Footer />
      <BottomNav />

      {/* 전역 오버레이 — 어느 페이지에서든 열립니다 */}
      <CartDrawer />
      <SearchOverlay />
    </div>
  );
}
