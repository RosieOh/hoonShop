import { createSlice } from '@reduxjs/toolkit';
import { loadState } from '@/utils/storage';

/**
 * 위시리스트는 서버 동기화가 원칙이지만, 비로그인 사용자도 담을 수 있어야 이탈이 줄어듭니다.
 * → 로컬에 먼저 쌓아두고 로그인 시점에 서버로 병합하는 구조를 전제로 합니다.
 */
const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState: {
    ids: loadState('wishlist', []),
    pendingSync: false,
  },
  reducers: {
    toggleWish: (state, action) => {
      const id = action.payload;
      state.ids = state.ids.includes(id)
        ? state.ids.filter((x) => x !== id)
        : [id, ...state.ids];
      state.pendingSync = true;
    },
    mergeServerWishlist: (state, action) => {
      state.ids = [...new Set([...action.payload, ...state.ids])];
      state.pendingSync = false;
    },
    clearWishlist: (state) => {
      state.ids = [];
    },
  },
});

export const { toggleWish, mergeServerWishlist, clearWishlist } = wishlistSlice.actions;
export default wishlistSlice.reducer;

export const selectWishIds = (state) => state.wishlist.ids;
export const selectIsWished = (id) => (state) => state.wishlist.ids.includes(id);
