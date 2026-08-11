import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';
import StoreLayout from '@/components/layout/StoreLayout';
import ScrollToTop from '@/components/layout/ScrollToTop';
import RequireAdmin from '@/features/admin/RequireAdmin';
import { ProductGridSkeleton } from '@/components/common/Skeleton';
import Home from '@/pages/Home';
import ProductListPage from '@/pages/ProductListPage';
import ProductDetailPage from '@/pages/ProductDetailPage';
import CartPage from '@/pages/CartPage';
import NotFoundPage from '@/pages/NotFoundPage';

// 결제 흐름과 관리자 구간은 첫 진입에 필요 없으므로 분리해 초기 번들을 가볍게 유지합니다.
const CheckoutPage = lazy(() => import('@/pages/CheckoutPage'));
const OrderCompletePage = lazy(() => import('@/pages/OrderCompletePage'));
const WishlistPage = lazy(() => import('@/pages/WishlistPage'));
const MyPage = lazy(() => import('@/pages/MyPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));

const AdminLayout = lazy(() => import('@/components/layout/AdminLayout'));
const AdminDashboard = lazy(() => import('@/pages/admin/AdminDashboard'));
const AdminOrders = lazy(() => import('@/pages/admin/AdminOrders'));
const AdminProducts = lazy(() => import('@/pages/admin/AdminProducts'));
const AdminInquiries = lazy(() => import('@/pages/admin/AdminInquiries'));

export default function App() {
  return (
    <>
      <ScrollToTop />

      <Suspense
        fallback={
          <div className="mx-auto max-w-[1240px] px-4 py-12 sm:px-6">
            <ProductGridSkeleton count={4} />
          </div>
        }
      >
        <Routes>
          {/* 고객 화면 */}
          <Route element={<StoreLayout />}>
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
          </Route>

          {/* 관리자 화면 — 헤더/푸터/하단 내비 없이 독립된 셸을 씁니다 */}
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <AdminLayout />
              </RequireAdmin>
            }
          >
            <Route index element={<AdminDashboard />} />
            <Route path="orders" element={<AdminOrders />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="inquiries" element={<AdminInquiries />} />
          </Route>
        </Routes>
      </Suspense>
    </>
  );
}
