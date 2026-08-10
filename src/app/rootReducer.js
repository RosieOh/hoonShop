import { combineReducers } from '@reduxjs/toolkit';

import { authApi } from '@/features/auth/authApi';
import { productApi } from '@/features/products/productApi';
import { searchApi } from '@/features/search/searchApi';
import { orderApi } from '@/features/order/orderApi';
import { promoApi } from '@/features/marketing/promoApi';
import { reviewApi } from '@/features/cs/reviewApi';

import authReducer from '@/features/auth/authSlice';
import cartReducer from '@/features/cart/cartSlice';
import wishlistReducer from '@/features/cart/wishlistSlice';
import searchReducer from '@/features/search/searchSlice';
import orderReducer from '@/features/order/orderSlice';
import paymentReducer from '@/features/payment/paymentSlice';
import promoReducer from '@/features/marketing/promoSlice';

export const apis = [authApi, productApi, searchApi, orderApi, promoApi, reviewApi];

export const rootReducer = combineReducers({
  // 🌐 서버 상태 (RTK Query) — 캐시·무효화·재검증은 여기서 담당
  [authApi.reducerPath]: authApi.reducer,
  [productApi.reducerPath]: productApi.reducer,
  [searchApi.reducerPath]: searchApi.reducer,
  [orderApi.reducerPath]: orderApi.reducer,
  [promoApi.reducerPath]: promoApi.reducer,
  [reviewApi.reducerPath]: reviewApi.reducer,

  // 💻 클라이언트 상태 (Slices) — 서버가 모르는 UI/의사결정 상태만
  auth: authReducer,
  cart: cartReducer,
  wishlist: wishlistReducer,
  search: searchReducer,
  order: orderReducer,
  payment: paymentReducer,
  promotion: promoReducer,
});
