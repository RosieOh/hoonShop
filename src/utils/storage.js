const PREFIX = 'hoonshop:';

/** localStorage 접근 실패(시크릿 모드, 용량 초과)를 앱 전체로 전파시키지 않습니다. */
export function loadState(key, fallback) {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

export function saveState(key, value) {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    /* 저장 실패해도 사용자 흐름은 계속되어야 합니다 */
  }
}

export function removeState(key) {
  try {
    localStorage.removeItem(PREFIX + key);
  } catch {
    /* noop */
  }
}
