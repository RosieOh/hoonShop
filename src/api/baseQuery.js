import { fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { logout, tokenRefreshed } from '@/features/auth/authSlice';

const rawBaseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    // 모든 요청 헤더에 토큰 주입 (도메인별로 중복 구현하지 않습니다)
    const token = getState().auth.token;
    if (token) headers.set('authorization', `Bearer ${token}`);
    return headers;
  },
});

/** 동시에 401이 여러 건 터져도 refresh는 한 번만 나가도록 잠급니다. */
let refreshPromise = null;

/**
 * 401 → refresh token으로 액세스 토큰 재발급 → 원 요청 1회 재시도.
 * 재발급까지 실패하면 로그아웃 처리합니다.
 */
export const baseQueryWithReauth = async (args, api, extraOptions) => {
  let result = await rawBaseQuery(args, api, extraOptions);

  if (result.error?.status === 401 && api.getState().auth.refreshToken) {
    refreshPromise ??= rawBaseQuery(
      { url: '/auth/refresh', method: 'POST', body: { refreshToken: api.getState().auth.refreshToken } },
      api,
      extraOptions,
    ).finally(() => {
      refreshPromise = null;
    });

    const refreshed = await refreshPromise;

    if (refreshed?.data?.token) {
      api.dispatch(tokenRefreshed(refreshed.data.token));
      result = await rawBaseQuery(args, api, extraOptions);
    } else {
      api.dispatch(logout());
    }
  }

  return result;
};
