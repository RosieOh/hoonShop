import { createSlice } from '@reduxjs/toolkit';

export const ORDER_STEPS = [
  { id: 'CHECKOUT', label: '주문서 작성' },
  { id: 'PAYMENT', label: '결제' },
  { id: 'COMPLETE', label: '완료' },
];

const emptyAddress = {
  recipient: '',
  phone: '',
  zipcode: '',
  address1: '',
  address2: '',
};

const orderSlice = createSlice({
  name: 'order',
  initialState: {
    step: 'CHECKOUT',
    /** 저장된 배송지를 고른 경우 그 id, 신규 입력이면 'new' */
    selectedAddressId: null,
    shippingAddress: emptyAddress,
    deliveryMemo: '',
    /** 서버에서 발급된 주문 번호 */
    orderId: null,
    completedOrder: null,
    /** 결제 직전 재고 검증 결과 */
    stockIssues: [],
  },
  reducers: {
    setStep: (state, action) => {
      state.step = action.payload;
    },
    selectAddress: (state, action) => {
      const address = action.payload;
      state.selectedAddressId = address.id;
      state.shippingAddress = {
        recipient: address.recipient,
        phone: address.phone,
        zipcode: address.zipcode,
        address1: address.address1,
        address2: address.address2,
      };
    },
    startNewAddress: (state) => {
      state.selectedAddressId = 'new';
      state.shippingAddress = emptyAddress;
    },
    updateShippingField: (state, action) => {
      const { field, value } = action.payload;
      state.shippingAddress[field] = value;
    },
    setDeliveryMemo: (state, action) => {
      state.deliveryMemo = action.payload;
    },
    setStockIssues: (state, action) => {
      state.stockIssues = action.payload;
    },
    orderCreated: (state, action) => {
      state.orderId = action.payload.id;
      state.step = 'PAYMENT';
    },
    orderCompleted: (state, action) => {
      state.completedOrder = action.payload;
      state.step = 'COMPLETE';
    },
    resetOrder: (state) => {
      state.step = 'CHECKOUT';
      state.orderId = null;
      state.completedOrder = null;
      state.stockIssues = [];
      state.deliveryMemo = '';
    },
  },
});

export const {
  setStep,
  selectAddress,
  startNewAddress,
  updateShippingField,
  setDeliveryMemo,
  setStockIssues,
  orderCreated,
  orderCompleted,
  resetOrder,
} = orderSlice.actions;

export default orderSlice.reducer;
