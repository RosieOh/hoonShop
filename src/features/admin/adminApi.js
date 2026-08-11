import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';
import { productApi } from '@/features/products/productApi';

export const adminApi = createApi({
  reducerPath: 'adminApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['AdminStats', 'AdminOrder', 'AdminProduct', 'AdminInquiry'],
  endpoints: (builder) => ({
    getStats: builder.query({
      query: () => '/admin/stats',
      providesTags: ['AdminStats'],
    }),

    getAdminOrders: builder.query({
      query: (params = {}) => ({ url: '/admin/orders', params }),
      providesTags: ['AdminOrder'],
    }),

    updateOrderStatus: builder.mutation({
      query: ({ id, status }) => ({ url: `/admin/orders/${id}`, method: 'PATCH', body: { status } }),
      /**
       * 낙관적 업데이트.
       * 상태 변경은 목록에서 연속으로 누르는 작업이라, 응답을 기다리며 멈추면
       * 관리자가 같은 버튼을 두 번 누르게 됩니다. 실패하면 되돌립니다.
       */
      onQueryStarted: async ({ id, status, queryArgs }, { dispatch, queryFulfilled }) => {
        const patch = dispatch(
          adminApi.util.updateQueryData('getAdminOrders', queryArgs, (draft) => {
            const order = draft.items.find((o) => o.id === id);
            if (order) order.status = status;
          }),
        );
        try {
          await queryFulfilled;
        } catch {
          patch.undo();
        }
      },
      invalidatesTags: ['AdminStats'],
    }),

    getAdminProducts: builder.query({
      query: (params = {}) => ({ url: '/admin/products', params }),
      providesTags: ['AdminProduct'],
    }),

    updateProduct: builder.mutation({
      query: ({ id, queryArgs: _q, ...patch }) => ({
        url: `/admin/products/${id}`,
        method: 'PATCH',
        body: patch,
      }),
      /**
       * 목록을 무효화하지 않고 캐시를 제자리에서 고칩니다.
       * 서버가 재고 오름차순으로 정렬해 내려주기 때문에, 재고를 고친 직후 재조회하면
       * 방금 수정한 행이 목록 저 아래로 튀어 사라집니다 — 편집 중인 위치를 잃는 건
       * 관리 화면에서 가장 짜증나는 동작입니다. 정렬은 다음 진입 때 갱신되면 충분합니다.
       */
      onQueryStarted: async ({ id, queryArgs = {} }, { dispatch, queryFulfilled }) => {
        const { data: updated } = await queryFulfilled;
        dispatch(
          adminApi.util.updateQueryData('getAdminProducts', queryArgs, (draft) => {
            const index = draft.items.findIndex((p) => p.id === id);
            if (index !== -1) draft.items[index] = updated;
          }),
        );
        // 스토어프론트 상품 캐시는 도메인이 달라 별도로 무효화합니다.
        dispatch(
          productApi.util.invalidateTags([
            { type: 'Product', id },
            { type: 'Product', id: 'LIST' },
          ]),
        );
      },
      invalidatesTags: ['AdminStats'],
    }),

    getInquiries: builder.query({
      query: (params = {}) => ({ url: '/admin/inquiries', params }),
      providesTags: ['AdminInquiry'],
    }),

    answerInquiry: builder.mutation({
      query: ({ id, answer }) => ({
        url: `/admin/inquiries/${id}/answer`,
        method: 'POST',
        body: { answer },
      }),
      invalidatesTags: ['AdminInquiry', 'AdminStats'],
    }),
  }),
});

export const {
  useGetStatsQuery,
  useGetAdminOrdersQuery,
  useUpdateOrderStatusMutation,
  useGetAdminProductsQuery,
  useUpdateProductMutation,
  useGetInquiriesQuery,
  useAnswerInquiryMutation,
} = adminApi;
