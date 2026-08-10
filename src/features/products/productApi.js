import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';

export const productApi = createApi({
  reducerPath: 'productApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Product'],
  keepUnusedDataFor: 120,
  endpoints: (builder) => ({
    getProducts: builder.query({
      query: (params = {}) => ({ url: '/products', params }),
      /**
       * 무한 스크롤: page를 제외한 조건이 같으면 같은 캐시 키로 묶고,
       * 새 페이지 결과를 뒤에 이어붙입니다. (필터가 바뀌면 캐시가 새로 생성됨)
       */
      serializeQueryArgs: ({ queryArgs, endpointName }) => {
        const { page: _page, ...rest } = queryArgs ?? {};
        return `${endpointName}(${JSON.stringify(rest)})`;
      },
      merge: (cache, incoming, { arg }) => {
        if ((arg?.page ?? 1) === 1) return incoming;
        cache.items.push(...incoming.items);
        cache.page = incoming.page;
        cache.hasNext = incoming.hasNext;
      },
      forceRefetch: ({ currentArg, previousArg }) => currentArg?.page !== previousArg?.page,
      providesTags: (result) =>
        result
          ? [...result.items.map(({ id }) => ({ type: 'Product', id })), { type: 'Product', id: 'LIST' }]
          : [{ type: 'Product', id: 'LIST' }],
    }),

    getProductDetail: builder.query({
      query: (id) => `/products/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Product', id }],
    }),
  }),
});

export const { useGetProductsQuery, useGetProductDetailQuery } = productApi;
