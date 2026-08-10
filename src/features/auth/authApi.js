import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '@/api/baseQuery';
import { setCredentials } from './authSlice';

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Me'],
  endpoints: (builder) => ({
    login: builder.mutation({
      query: (credentials) => ({ url: '/auth/login', method: 'POST', body: credentials }),
      // 응답이 도착하는 즉시 스토어에 반영 → 컴포넌트에서 별도 dispatch가 필요 없습니다.
      onQueryStarted: async (_arg, { dispatch, queryFulfilled }) => {
        const { data } = await queryFulfilled;
        dispatch(setCredentials(data));
      },
      invalidatesTags: ['Me'],
    }),
    me: builder.query({
      query: () => '/auth/me',
      providesTags: ['Me'],
    }),
  }),
});

export const { useLoginMutation, useMeQuery } = authApi;
