import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Check, Info, TriangleAlert } from 'lucide-react';
import { cn } from '@/utils/format';

const ToastContext = createContext(null);

const ICONS = { success: Check, error: TriangleAlert, info: Info };

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const seq = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback(
    (message, { tone = 'success', duration = 2600, action } = {}) => {
      const id = (seq.current += 1);
      setToasts((list) => [...list.slice(-2), { id, message, tone, action }]);
      setTimeout(() => dismiss(id), duration);
      return id;
    },
    [dismiss],
  );

  const value = useMemo(() => ({ toast, dismiss }), [toast, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {createPortal(
        <div
          // 스크린리더가 흐름을 끊지 않고 읽도록 polite
          role="status"
          aria-live="polite"
          className="pointer-events-none fixed inset-x-0 bottom-24 z-[60] flex flex-col items-center gap-2 px-4 sm:bottom-8"
        >
          {toasts.map((t) => {
            const Icon = ICONS[t.tone] ?? Info;
            return (
              <div
                key={t.id}
                className={cn(
                  'hs-rise pointer-events-auto flex w-full max-w-sm items-center gap-2.5 rounded-pill px-4 py-3 shadow-pop',
                  t.tone === 'error' ? 'bg-danger text-white' : 'bg-ink text-canvas',
                )}
              >
                <Icon size={16} className="shrink-0" aria-hidden="true" />
                <p className="min-w-0 flex-1 text-[14px] leading-snug">{t.message}</p>
                {t.action && (
                  <button
                    type="button"
                    onClick={() => {
                      t.action.onClick();
                      dismiss(t.id);
                    }}
                    className="shrink-0 rounded-pill px-3 py-1 text-[13px] font-semibold underline underline-offset-2"
                  >
                    {t.action.label}
                  </button>
                )}
              </div>
            );
          })}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast는 <ToastProvider> 안에서만 사용할 수 있습니다.');
  return ctx;
}
