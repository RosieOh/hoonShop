import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@/utils/format';
import { useBodyScrollLock } from '@/hooks/useBodyScrollLock';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * 오버레이 계열(우측 드로어 / 모바일 바텀시트 / 센터 모달)의 공통 껍데기.
 *
 * 접근성 처리:
 *  · role="dialog" + aria-modal + aria-labelledby
 *  · Esc로 닫기, 배경 클릭으로 닫기
 *  · 포커스를 시트 안에 가두고, 닫을 때 직전 요소로 되돌립니다
 *  · 배경 스크롤 잠금
 */
export default function Sheet({
  open,
  onClose,
  title,
  description,
  side = 'right', // right | bottom | center
  width = 'max-w-md',
  footer,
  children,
  className,
}) {
  const panelRef = useRef(null);
  const restoreRef = useRef(null);

  useBodyScrollLock(open);

  useEffect(() => {
    if (!open) return undefined;

    restoreRef.current = document.activeElement;
    const panel = panelRef.current;
    // 첫 포커스는 패널 자체로 — 내부 첫 버튼으로 보내면 맥락 없이 튀어 보입니다.
    panel?.focus();

    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
        return;
      }
      if (e.key !== 'Tab' || !panel) return;

      const items = [...panel.querySelectorAll(FOCUSABLE)].filter(
        (el) => el.offsetParent !== null,
      );
      if (!items.length) return;

      const first = items[0];
      const last = items[items.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      restoreRef.current?.focus?.();
    };
  }, [open, onClose]);

  if (!open) return null;

  const position = {
    right: 'right-0 top-0 h-full w-full sm:w-auto hs-slide-left',
    bottom: 'inset-x-0 bottom-0 max-h-[88vh] rounded-t-lg hs-rise',
    center: 'inset-0 m-auto h-fit max-h-[86vh] rounded-lg hs-pop',
  }[side];

  const titleId = `sheet-title-${title?.replace(/\s/g, '') ?? 'x'}`;

  return createPortal(
    <div className="fixed inset-0 z-50">
      <button
        type="button"
        className="hs-fade absolute inset-0 h-full w-full cursor-default bg-ink/35 backdrop-blur-[2px]"
        onClick={onClose}
        aria-label="닫기"
        tabIndex={-1}
      />

      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={cn(
          'absolute flex flex-col bg-surface shadow-pop outline-none',
          side === 'right' && width,
          side === 'center' && `w-[calc(100%-2rem)] ${width}`,
          position,
          className,
        )}
      >
        <header className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
          <div className="min-w-0">
            <h2 id={titleId} className="font-display text-[22px] leading-tight font-semibold">
              {title}
            </h2>
            {description && <p className="mt-1 text-[13px] text-ink-soft">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label={`${title} 닫기`}
            className="-mr-2 -mt-1 flex size-11 shrink-0 items-center justify-center rounded-full text-ink transition-colors hover:bg-canvas"
          >
            <X size={20} aria-hidden="true" />
          </button>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">{children}</div>

        {footer && <div className="border-t border-line bg-surface px-5 py-4">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}
