import { forwardRef } from 'react';
import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/format';

const VARIANTS = {
  primary:
    'bg-primary text-on-primary hover:bg-primary-hover active:bg-primary-hover disabled:bg-line disabled:text-ink-soft',
  outline:
    'bg-surface text-ink border border-line-strong hover:border-ink hover:bg-canvas disabled:text-ink-faint disabled:border-line',
  ghost: 'bg-transparent text-ink hover:bg-primary-soft disabled:text-ink-faint',
  soft: 'bg-primary-soft text-primary hover:bg-[#f1dfe6] disabled:text-ink-faint',
  danger: 'bg-danger text-white hover:brightness-90',
};

/** 터치 타깃 44px 하한을 지키기 위해 sm도 40px 이상으로 둡니다. */
const SIZES = {
  sm: 'h-10 px-4 text-[13px]',
  md: 'h-12 px-6 text-[15px]',
  lg: 'h-14 px-8 text-base',
};

const Button = forwardRef(function Button(
  {
    as,
    to,
    variant = 'primary',
    size = 'md',
    full = false,
    loading = false,
    icon: Icon,
    iconRight = false,
    className,
    children,
    disabled,
    ...rest
  },
  ref,
) {
  const Component = to ? Link : (as ?? 'button');

  const classes = cn(
    'relative inline-flex items-center justify-center gap-2 rounded-pill font-medium tracking-[-0.01em]',
    'transition-[background-color,color,border-color,transform] duration-200 ease-out-soft',
    'active:scale-[0.98] disabled:active:scale-100 disabled:cursor-not-allowed',
    VARIANTS[variant],
    SIZES[size],
    full && 'w-full',
    className,
  );

  const content = (
    <>
      {loading && <Loader2 size={16} className="animate-spin" aria-hidden="true" />}
      {!loading && Icon && !iconRight && <Icon size={17} aria-hidden="true" />}
      <span className={cn(loading && 'opacity-90')}>{children}</span>
      {!loading && Icon && iconRight && <Icon size={17} aria-hidden="true" />}
    </>
  );

  if (Component === Link) {
    return (
      <Link ref={ref} to={to} className={classes} aria-disabled={disabled} {...rest}>
        {content}
      </Link>
    );
  }

  return (
    <Component
      ref={ref}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {content}
    </Component>
  );
});

export default Button;

/** 아이콘 전용 버튼 — aria-label이 없으면 스크린리더가 읽을 수 없으므로 필수 인자로 둡니다. */
export function IconButton({ label, icon: Icon, size = 20, className, badge, ...rest }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={cn(
        'relative inline-flex size-11 items-center justify-center rounded-full text-ink',
        'transition-colors duration-150 hover:bg-primary-soft active:bg-primary-soft',
        className,
      )}
      {...rest}
    >
      <Icon size={size} aria-hidden="true" strokeWidth={1.6} />
      {badge > 0 && (
        <span
          className="tnum absolute top-1 right-0.5 min-w-[19px] rounded-pill bg-primary px-1 py-0.5 text-[11px] leading-none font-semibold text-on-primary"
          aria-hidden="true"
        >
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </button>
  );
}
