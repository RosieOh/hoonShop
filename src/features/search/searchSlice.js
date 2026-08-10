import { createSlice } from '@reduxjs/toolkit';
import { loadState } from '@/utils/storage';

const MAX_RECENT = 8;

const searchSlice = createSlice({
  name: 'search',
  initialState: {
    recent: loadState('recentSearch', []),
    isOverlayOpen: false,
  },
  reducers: {
    pushRecent: (state, action) => {
      const keyword = action.payload.trim();
      if (!keyword) return;
      state.recent = [keyword, ...state.recent.filter((k) => k !== keyword)].slice(0, MAX_RECENT);
    },
    removeRecent: (state, action) => {
      state.recent = state.recent.filter((k) => k !== action.payload);
    },
    clearRecent: (state) => {
      state.recent = [];
    },
    openSearch: (state) => {
      state.isOverlayOpen = true;
    },
    closeSearch: (state) => {
      state.isOverlayOpen = false;
    },
  },
});

export const { pushRecent, removeRecent, clearRecent, openSearch, closeSearch } =
  searchSlice.actions;
export default searchSlice.reducer;
