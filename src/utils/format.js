const KRW = new Intl.NumberFormat('ko-KR');

/** 12000 → "12,000" */
export const formatPrice = (value) => KRW.format(Math.round(Number(value) || 0));

/** 12000 → "12,000원" */
export const formatWon = (value) => `${formatPrice(value)}원`;

/** 큰 금액 축약: 12,340,000 → "1,234만" · 축 눈금과 통계 타일용 */
export function formatCompactWon(value) {
  const n = Math.round(Number(value) || 0);
  if (n >= 100_000_000) return `${(n / 100_000_000).toFixed(1)}억`;
  if (n >= 10_000) return `${KRW.format(Math.round(n / 10_000))}만`;
  return KRW.format(n);
}

/** ISO → "2026.08.11" */
export function formatDate(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '-';
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(
    d.getDate(),
  ).padStart(2, '0')}`;
}

/** ISO → "08.11 (화)" — 차트 x축·표 헤더용 */
export function formatShortDate(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '-';
  const weekday = ['일', '월', '화', '수', '목', '금', '토'][d.getDay()];
  return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')} (${weekday})`;
}

/** 상대 시간: "3일 전" */
export function formatRelative(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  const day = Math.floor(diff / 86_400_000);
  if (day < 1) return '오늘';
  if (day < 7) return `${day}일 전`;
  if (day < 30) return `${Math.floor(day / 7)}주 전`;
  return formatDate(iso);
}

/** 남은 시간 → { hours, minutes, seconds, done } */
export function countdown(targetIso, now = Date.now()) {
  const left = Math.max(0, new Date(targetIso).getTime() - now);
  return {
    hours: String(Math.floor(left / 3_600_000)).padStart(2, '0'),
    minutes: String(Math.floor((left % 3_600_000) / 60_000)).padStart(2, '0'),
    seconds: String(Math.floor((left % 60_000) / 1000)).padStart(2, '0'),
    done: left === 0,
  };
}

/** 전화번호 자동 하이픈 */
export const formatPhone = (v) =>
  v
    .replace(/\D/g, '')
    .slice(0, 11)
    .replace(/^(\d{3})(\d{3,4})(\d{0,4}).*/, (_, a, b, c) => [a, b, c].filter(Boolean).join('-'));

/** 카드번호 4자리 그룹 */
export const formatCardNumber = (v) =>
  v
    .replace(/\D/g, '')
    .slice(0, 16)
    .replace(/(\d{4})(?=\d)/g, '$1 ')
    .trim();

/** 클래스 병합 (falsy 제거) */
export const cn = (...classes) => classes.filter(Boolean).join(' ');
