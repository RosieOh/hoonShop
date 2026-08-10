import { cn } from '@/utils/format';

const TONES = {
  new: 'bg-ink text-canvas',
  best: 'bg-primary text-on-primary',
  limited: 'bg-accent-soft text-accent-ink ring-1 ring-accent/40',
  sale: 'bg-sale text-white',
  soldout: 'bg-line text-ink-soft',
  info: 'bg-primary-soft text-primary',
  success: 'bg-success-soft text-success',
};

const LABELS = {
  new: 'NEW',
  best: 'BEST',
  limited: 'LIMITED',
  soldout: 'SOLD OUT',
};

export default function Badge({ tone = 'info', children, className }) {
  return (
    <span
      className={cn(
        'font-label inline-flex items-center rounded-sm px-1.5 py-1 text-[10px] leading-none font-semibold tracking-[0.12em] uppercase',
        TONES[tone] ?? TONES.info,
        className,
      )}
    >
      {children ?? LABELS[tone] ?? tone}
    </span>
  );
}
