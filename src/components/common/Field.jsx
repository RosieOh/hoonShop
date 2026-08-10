import { useId } from 'react';
import { AlertCircle } from 'lucide-react';
import { cn } from '@/utils/format';

/**
 * 폼 필드.
 * 라벨은 항상 보입니다 — placeholder를 라벨 대신 쓰면 입력을 시작한 순간
 * 무엇을 적는 칸이었는지 사라집니다.
 * 에러는 필드 바로 아래에, aria-describedby로 연결해 스크린리더에도 전달합니다.
 */
export default function Field({
  label,
  error,
  hint,
  required,
  className,
  inputClassName,
  as = 'input',
  children,
  ...rest
}) {
  const id = useId();
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;
  const Component = as;

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="flex items-center gap-1 text-[13px] font-medium text-ink">
        {label}
        {required && (
          <span className="text-sale" aria-label="필수 입력">
            *
          </span>
        )}
      </label>

      <Component
        id={id}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={cn(error && errorId, hint && hintId) || undefined}
        aria-required={required || undefined}
        className={cn(
          'w-full rounded-md border bg-surface px-3.5 text-[15px] text-ink placeholder:text-ink-faint',
          'transition-colors duration-150 outline-none',
          as === 'textarea' ? 'min-h-24 resize-y py-3' : 'h-12',
          as === 'select' && 'appearance-none pr-9',
          error
            ? 'border-danger focus:border-danger'
            : 'border-line focus:border-primary hover:border-line-strong',
          inputClassName,
        )}
        {...rest}
      >
        {children}
      </Component>

      {hint && !error && (
        <p id={hintId} className="text-[12px] text-ink-soft">
          {hint}
        </p>
      )}

      {error && (
        <p id={errorId} role="alert" className="flex items-start gap-1.5 text-[12px] text-danger">
          <AlertCircle size={13} className="mt-px shrink-0" aria-hidden="true" />
          {error}
        </p>
      )}
    </div>
  );
}
