import Button from './Button';

/**
 * 빈 상태는 "없음"을 알리는 데서 끝나면 안 되고,
 * 다음에 무엇을 할 수 있는지를 항상 제안해야 합니다.
 */
export default function EmptyState({ icon: Icon, title, description, actionLabel, actionTo, onAction }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-20 text-center">
      {Icon && (
        <span className="mb-5 flex size-16 items-center justify-center rounded-full bg-primary-soft text-primary">
          <Icon size={26} strokeWidth={1.5} aria-hidden="true" />
        </span>
      )}
      <p className="font-display text-[24px] leading-snug font-semibold text-ink">{title}</p>
      {description && <p className="mt-2 max-w-sm text-[14px] text-ink-soft">{description}</p>}
      {(actionTo || onAction) && (
        <Button to={actionTo} onClick={onAction} variant="outline" className="mt-7">
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
