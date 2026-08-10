import { createSlice } from '@reduxjs/toolkit';
import { loadState, removeState } from '@/utils/storage';

const persisted = loadState('auth', {});

const initialState = {
  user: persisted.user ?? null,
  token: persisted.token ?? null,
  refreshToken: persisted.refreshToken ?? null,
  isAuthenticated: Boolean(persisted.token),
  /** 로그인이 필요해 가로챈 경로. 로그인 후 이 곳으로 돌려보냅니다. */
  redirectTo: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action) => {
      const { user, token, refreshToken } = action.payload;
      state.user = user;
      state.token = token;
      state.refreshToken = refreshToken ?? null;
      state.isAuthenticated = true;
    },
    tokenRefreshed: (state, action) => {
      state.token = action.payload;
    },
    setRedirectTo: (state, action) => {
      state.redirectTo = action.payload;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      removeState('auth');
    },
  },
});

export const { setCredentials, tokenRefreshed, setRedirectTo, logout } = authSlice.actions;
export default authSlice.reducer;

export const selectIsAuthenticated = (state) => state.auth.isAuthenticated;
export const selectUser = (state) => state.auth.user;
