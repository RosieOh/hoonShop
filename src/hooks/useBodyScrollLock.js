import { useEffect } from 'react';

/**
 * 모달/드로어가 열려 있는 동안 배경 스크롤을 잠급니다.
 * 스크롤바 폭만큼 패딩을 보정해 레이아웃이 튀지 않게 합니다.
 */
export function useBodyScrollLock(active) {
  useEffect(() => {
    if (!active) return undefined;

    const { body } = document;
    const prevOverflow = body.style.overflow;
    const prevPadding = body.style.paddingRight;
    const gap = window.innerWidth - document.documentElement.clientWidth;

    body.style.overflow = 'hidden';
    if (gap > 0) body.style.paddingRight = `${gap}px`;

    return () => {
      body.style.overflow = prevOverflow;
      body.style.paddingRight = prevPadding;
    };
  }, [active]);
}
