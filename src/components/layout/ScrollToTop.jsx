import { useEffect } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

/**
 * 라우트가 바뀌면 상단으로 이동합니다.
 * 단, 뒤로가기(POP)일 때는 브라우저가 복원한 스크롤 위치를 존중합니다 —
 * 목록으로 돌아왔는데 맨 위로 튀면 보던 자리를 다시 찾아야 합니다.
 */
export default function ScrollToTop() {
  const { pathname } = useLocation();
  const navigationType = useNavigationType();

  useEffect(() => {
    if (navigationType === 'POP') return;
    window.scrollTo({ top: 0, behavior: 'instant' });
  }, [pathname, navigationType]);

  return null;
}
