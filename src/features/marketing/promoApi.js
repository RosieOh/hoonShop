import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';

export const promoApi = createApi({
  reducerPath: 'promoApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Coupon'],
  endpoints: (builder) => ({
    getPromotions: builder.query({
      query: () => '/promotions',
    }),
    getCoupons: builder.query({
      query: () => '/coupons',
      providesTags: ['Coupon'],
    }),
  }),
});

export const { useGetPromotionsQuery, useGetCouponsQuery } = promoApi;
