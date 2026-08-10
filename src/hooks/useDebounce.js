import { useEffect, useState } from 'react';

/**
 * 값이 delay 동안 멈춰야 반영됩니다.
 * 검색 자동완성에서 타이핑마다 요청이 나가는 것을 막는 용도.
 */
export function useDebounce(value, delay = 250) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(id);
  }, [value, delay]);

  return debounced;
}
