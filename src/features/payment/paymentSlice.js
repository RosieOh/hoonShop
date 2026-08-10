import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

export const PAYMENT_METHODS = [
  { id: 'CARD', label: '신용·체크카드' },
  { id: 'TRANSFER', label: '실시간 계좌이체' },
  { id: 'VIRTUAL', label: '가상계좌' },
  { id: 'EASY', label: '간편결제' },
];

/**
 * 결제 승인.
 * 실제 서비스에서는 PG SDK(토스페이먼츠·포트원)가 반환한 paymentKey를 서버로 넘겨
 * 서버가 금액을 검증한 뒤 승인합니다. 프론트에서 계산한 금액을 그대로 신뢰하면 안 됩니다.
 */
export const confirmPayment = createAsyncThunk(
  'payment/confirm',
  async (paymentData, { rejectWithValue }) => {
    try {
      const response = await fetch('/api/payments/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(paymentData),
      });
      const data = await response.json();
      if (!response.ok) {
        return rejectWithValue({
          code: data.code ?? 'UNKNOWN',
          message: data.message ?? '결제 처리 중 문제가 발생했습니다.',
        });
      }
      return data;
    } catch {
      return rejectWithValue({
        code: 'NETWORK',
        message: '네트워크 연결을 확인한 뒤 다시 시도해 주세요.',
      });
    }
  },
);

const paymentSlice = createSlice({
  name: 'payment',
  initialState: {
    method: 'CARD',
    status: 'idle', // idle | loading | succeeded | failed
    error: null,
    receipt: null,
    agreements: { terms: false, privacy: false },
  },
  reducers: {
    setMethod: (state, action) => {
      state.method = action.payload;
      state.error = null;
    },
    toggleAgreement: (state, action) => {
      const key = action.payload;
      state.agreements[key] = !state.agreements[key];
    },
    setAllAgreements: (state, action) => {
      state.agreements = { terms: action.payload, privacy: action.payload };
    },
    resetPayment: (state) => {
      state.status = 'idle';
      state.error = null;
      state.receipt = null;
      state.agreements = { terms: false, privacy: false };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(confirmPayment.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(confirmPayment.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.receipt = action.payload;
      })
      .addCase(confirmPayment.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? { code: 'UNKNOWN', message: '결제에 실패했습니다.' };
      });
  },
});

export const { setMethod, toggleAgreement, setAllAgreements, resetPayment } = paymentSlice.actions;
export default paymentSlice.reducer;

export const selectCanPay = (state) =>
  state.payment.agreements.terms && state.payment.agreements.privacy;
