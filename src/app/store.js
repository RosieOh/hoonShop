import { configureStore } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import { apis, rootReducer } from './rootReducer';
import { persistMiddleware } from './persistMiddleware';

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefault) =>
    getDefault()
      .prepend(persistMiddleware.middleware)
      .concat(apis.map((api) => api.middleware)),
  devTools: import.meta.env.DEV,
});

// 네트워크 복구 / 탭 포커스 시 자동 재검증
setupListeners(store.dispatch);
