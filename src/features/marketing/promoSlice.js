import { createSlice } from '@reduxjs/toolkit';
import { loadState } from '@/utils/storage';

const promoSlice = createSlice({
  name: 'promotion',
  initialState: {
    /** 오늘 하루 배너 닫기 — 날짜가 바뀌면 다시 노출됩니다. */
    bannerDismissedOn: loadState('bannerDismissedOn', null),
    appliedCouponIds: [],
    isCouponSheetOpen: false,
  },
  reducers: {
    dismissBanner: (state) => {
      state.bannerDismissedOn = new Date().toDateString();
    },
    applyCoupons: (state, action) => {
      state.appliedCouponIds = action.payload;
    },
    clearCoupons: (state) => {
      state.appliedCouponIds = [];
    },
    openCouponSheet: (state) => {
      state.isCouponSheetOpen = true;
    },
    closeCouponSheet: (state) => {
      state.isCouponSheetOpen = false;
    },
  },
});

export const {
  dismissBanner,
  applyCoupons,
  clearCoupons,
  openCouponSheet,
  closeCouponSheet,
} = promoSlice.actions;

export default promoSlice.reducer;

export const selectShowBanner = (state) =>
  state.promotion.bannerDismissedOn !== new Date().toDateString();
