import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';

export const orderApi = createApi({
  reducerPath: 'orderApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Order', 'Address'],
  endpoints: (builder) => ({
    getAddresses: builder.query({
      query: () => '/addresses',
      providesTags: ['Address'],
    }),
    /** 결제 승인 전 최종 재고 확인 */
    validateStock: builder.mutation({
      query: (body) => ({ url: '/orders/validate', method: 'POST', body }),
    }),
    createOrder: builder.mutation({
      query: (body) => ({ url: '/orders', method: 'POST', body }),
      invalidatesTags: ['Order'],
    }),
    getOrders: builder.query({
      query: () => '/orders',
      providesTags: ['Order'],
    }),
    getOrder: builder.query({
      query: (id) => `/orders/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Order', id }],
    }),
  }),
});

export const {
  useGetAddressesQuery,
  useValidateStockMutation,
  useCreateOrderMutation,
  useGetOrdersQuery,
  useGetOrderQuery,
} = orderApi;
