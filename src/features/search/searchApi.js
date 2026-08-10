import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';

export const searchApi = createApi({
  reducerPath: 'searchApi',
  baseQuery: baseQueryWithReauth,
  endpoints: (builder) => ({
    getSuggestions: builder.query({
      query: (q) => ({ url: '/search/suggestions', params: { q } }),
      // 자동완성은 짧게만 캐싱 — 오래 들고 있어도 가치가 없습니다.
      keepUnusedDataFor: 30,
    }),
    getTrending: builder.query({
      query: () => '/search/trending',
      keepUnusedDataFor: 600,
    }),
  }),
});

export const { useGetSuggestionsQuery, useGetTrendingQuery } = searchApi;
