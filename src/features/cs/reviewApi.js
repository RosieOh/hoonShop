import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';

export const reviewApi = createApi({
  reducerPath: 'reviewApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Review', 'Qna'],
  endpoints: (builder) => ({
    getReviews: builder.query({
      query: ({ productId, sort = 'recent' }) => ({ url: '/reviews', params: { productId, sort } }),
      providesTags: (_r, _e, arg) => [{ type: 'Review', id: arg.productId }],
    }),
    createReview: builder.mutation({
      query: (body) => ({ url: '/reviews', method: 'POST', body }),
      invalidatesTags: (_r, _e, arg) => [{ type: 'Review', id: arg.productId }],
    }),
    getQna: builder.query({
      query: (productId) => ({ url: '/qna', params: { productId } }),
      providesTags: (_r, _e, id) => [{ type: 'Qna', id }],
    }),
  }),
});

export const { useGetReviewsQuery, useCreateReviewMutation, useGetQnaQuery } = reviewApi;
