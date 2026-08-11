import { Ban, Check, Hammer, Truck, Wallet } from 'lucide-react';
import { cn } from '@/utils/format';

/**
 * 주문 상태 배지.
 *
 * 상태를 색으로만 구분하지 않습니다 — 아이콘 + 한글 라벨이 항상 함께 붙습니다.
 * 색은 진행 단계(ordinal)를 보조로만 나타냅니다.
 */
export const STATUS_META = {
  PAID: { label: '결제 완료', icon: Wallet, className: 'bg-[#F7EDF1] text-[#7A3B52]' },
  MAKING: { label: '제작 중', icon: Hammer, className: 'bg-[#F3E6EB] text-[#7A3B52]' },
  SHIPPED: { label: '발송', icon: Truck, className: 'bg-[#EAE0F0] text-[#5B3B7A]' },
  DELIVERED: { label: '배송 완료', icon: Check, className: 'bg-success-soft text-success' },
  CANCELLED: { label: '취소', icon: Ban, className: 'bg-surface-sunken text-ink-soft' },
  PAYMENT_PENDING: { label: '결제 대기', icon: Wallet, className: 'bg-surface-sunken text-ink-soft' },
};

export default function StatusBadge({ status, className }) {
  const meta = STATUS_META[status] ?? STATUS_META.PAYMENT_PENDING;
  const Icon = meta.icon;

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-pill px-2.5 py-1 text-[12px] font-medium whitespace-nowrap',
        meta.className,
        className,
      )}
    >
      <Icon size={12} strokeWidth={2} aria-hidden="true" />
      {meta.label}
    </span>
  );
}
