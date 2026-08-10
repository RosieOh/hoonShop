import { createSelector, createSlice } from '@reduxjs/toolkit';
import { loadState } from '@/utils/storage';

/**
 * 장바구니 라인의 정체성은 상품ID가 아니라 "상품ID + 선택한 옵션"입니다.
 * 같은 목걸이라도 민트/42cm 와 라일락/38cm 는 별개의 줄로 관리해야 합니다.
 */
export const makeLineId = (productId, options = {}) =>
  [productId, options.color ?? '-', options.size ?? '-'].join('__');

const MAX_QTY = 10;

const initialState = {
  items: loadState('cart', []),
  /** 체크아웃 대상으로 선택된 lineId 목록 (전체 선택이 기본) */
  selected: loadState('cartSelected', []),
  isDrawerOpen: false,
  /** 담은 직후 짧게 노출되는 확인 토스트 */
  lastAdded: null,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    addToCart: {
      prepare: (product, options = {}, quantity = 1) => ({
        payload: { product, options, quantity },
      }),
      reducer: (state, action) => {
        const { product, options, quantity } = action.payload;
        const lineId = makeLineId(product.id, options);
        const existing = state.items.find((i) => i.lineId === lineId);

        if (existing) {
          existing.quantity = Math.min(MAX_QTY, existing.quantity + quantity);
        } else {
          state.items.unshift({
            lineId,
            productId: product.id,
            name: product.name,
            category: product.category,
            palette: product.palette,
            price: product.price,
            salePrice: product.salePrice,
            stock: product.stock,
            options,
            quantity: Math.min(MAX_QTY, quantity),
          });
          state.selected.push(lineId);
        }
        state.lastAdded = { lineId, name: product.name, at: Date.now() };
      },
    },

    /** 옵션 변경: 이미 같은 옵션의 줄이 있으면 수량을 합칩니다. */
    updateOptions: (state, action) => {
      const { lineId, options } = action.payload;
      const line = state.items.find((i) => i.lineId === lineId);
      if (!line) return;

      const nextOptions = { ...line.options, ...options };
      const nextId = makeLineId(line.productId, nextOptions);
      if (nextId === lineId) return;

      const duplicate = state.items.find((i) => i.lineId === nextId);
      if (duplicate) {
        duplicate.quantity = Math.min(MAX_QTY, duplicate.quantity + line.quantity);
        state.items = state.items.filter((i) => i.lineId !== lineId);
        state.selected = state.selected.filter((id) => id !== lineId);
        if (!state.selected.includes(nextId)) state.selected.push(nextId);
      } else {
        line.options = nextOptions;
        line.lineId = nextId;
        state.selected = state.selected.map((id) => (id === lineId ? nextId : id));
      }
    },

    updateQuantity: (state, action) => {
      const { lineId, quantity } = action.payload;
      const line = state.items.find((i) => i.lineId === lineId);
      if (!line) return;
      line.quantity = Math.min(MAX_QTY, Math.max(1, quantity));
    },

    removeFromCart: (state, action) => {
      state.items = state.items.filter((i) => i.lineId !== action.payload);
      state.selected = state.selected.filter((id) => id !== action.payload);
    },

    removeSelected: (state) => {
      state.items = state.items.filter((i) => !state.selected.includes(i.lineId));
      state.selected = [];
    },

    toggleSelect: (state, action) => {
      const lineId = action.payload;
      state.selected = state.selected.includes(lineId)
        ? state.selected.filter((id) => id !== lineId)
        : [...state.selected, lineId];
    },

    toggleSelectAll: (state) => {
      const allSelected = state.selected.length === state.items.length;
      state.selected = allSelected ? [] : state.items.map((i) => i.lineId);
    },

    clearCart: (state) => {
      state.items = [];
      state.selected = [];
    },

    openDrawer: (state) => {
      state.isDrawerOpen = true;
    },
    closeDrawer: (state) => {
      state.isDrawerOpen = false;
    },
    clearLastAdded: (state) => {
      state.lastAdded = null;
    },
  },
});

export const {
  addToCart,
  updateOptions,
  updateQuantity,
  removeFromCart,
  removeSelected,
  toggleSelect,
  toggleSelectAll,
  clearCart,
  openDrawer,
  closeDrawer,
  clearLastAdded,
} = cartSlice.actions;

export default cartSlice.reducer;

/* --------------------------------------------------------------- 셀렉터 --- */
export const selectCartItems = (state) => state.cart.items;
export const selectSelectedIds = (state) => state.cart.selected;

export const selectCartCount = createSelector(selectCartItems, (items) =>
  items.reduce((sum, i) => sum + i.quantity, 0),
);

export const selectSelectedItems = createSelector(
  [selectCartItems, selectSelectedIds],
  (items, selected) => items.filter((i) => selected.includes(i.lineId)),
);

const FREE_SHIPPING_THRESHOLD = 50_000;
const SHIPPING_FEE = 3_000;

/** 주문 금액 계산의 단일 출처. 장바구니/체크아웃/결제가 모두 이 값을 씁니다. */
export const selectCartSummary = createSelector(selectSelectedItems, (items) => {
  const listTotal = items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  const payTotal = items.reduce((sum, i) => sum + (i.salePrice ?? i.price) * i.quantity, 0);
  const itemDiscount = listTotal - payTotal;
  const shippingFee = payTotal === 0 || payTotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;

  return {
    count: items.reduce((sum, i) => sum + i.quantity, 0),
    listTotal,
    itemDiscount,
    payTotal,
    shippingFee,
    freeShippingGap: Math.max(0, FREE_SHIPPING_THRESHOLD - payTotal),
    freeShippingThreshold: FREE_SHIPPING_THRESHOLD,
  };
});
