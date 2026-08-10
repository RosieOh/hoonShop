import { createListenerMiddleware, isAnyOf } from '@reduxjs/toolkit';
import { saveState } from '@/utils/storage';
import {
  addToCart,
  clearCart,
  removeFromCart,
  removeSelected,
  toggleSelect,
  toggleSelectAll,
  updateOptions,
  updateQuantity,
} from '@/features/cart/cartSlice';
import { mergeServerWishlist, toggleWish } from '@/features/cart/wishlistSlice';
import { clearRecent, pushRecent, removeRecent } from '@/features/search/searchSlice';
import { logout, setCredentials, tokenRefreshed } from '@/features/auth/authSlice';
import { dismissBanner } from '@/features/marketing/promoSlice';

/**
 * 상태를 localStorage에 반영하는 단일 지점.
 * 리듀서 안에서 직접 저장하면 순수성이 깨지고 테스트가 어려워지므로 미들웨어로 분리했습니다.
 */
export const persistMiddleware = createListenerMiddleware();

persistMiddleware.startListening({
  matcher: isAnyOf(
    addToCart,
    updateOptions,
    updateQuantity,
    removeFromCart,
    removeSelected,
    toggleSelect,
    toggleSelectAll,
    clearCart,
  ),
  effect: (_action, api) => {
    const { items, selected } = api.getState().cart;
    saveState('cart', items);
    saveState('cartSelected', selected);
  },
});

persistMiddleware.startListening({
  matcher: isAnyOf(toggleWish, mergeServerWishlist),
  effect: (_action, api) => saveState('wishlist', api.getState().wishlist.ids),
});

persistMiddleware.startListening({
  matcher: isAnyOf(pushRecent, removeRecent, clearRecent),
  effect: (_action, api) => saveState('recentSearch', api.getState().search.recent),
});

persistMiddleware.startListening({
  matcher: isAnyOf(setCredentials, tokenRefreshed),
  effect: (_action, api) => {
    const { user, token, refreshToken } = api.getState().auth;
    saveState('auth', { user, token, refreshToken });
  },
});

persistMiddleware.startListening({
  actionCreator: logout,
  effect: () => saveState('auth', {}),
});

persistMiddleware.startListening({
  actionCreator: dismissBanner,
  effect: (_action, api) =>
    saveState('bannerDismissedOn', api.getState().promotion.bannerDismissedOn),
});
