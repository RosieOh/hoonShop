import { Link } from 'react-router-dom';
import { MessageCircle } from 'lucide-react';
import { CATEGORIES } from '@/mocks/db';

/** lucide v1에서 브랜드 아이콘이 빠져 인스타그램 글리프는 직접 그립니다. */
function InstagramGlyph(props) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <rect x="2" y="2" width="20" height="20" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

const HELP = [
  ['배송 안내', '/help/shipping'],
  ['교환·반품', '/help/returns'],
  ['사이즈 가이드', '/help/size'],
  ['자주 묻는 질문', '/help/faq'],
];

export default function Footer() {
  return (
    <footer className="mt-24 border-t border-line bg-surface">
      <div className="mx-auto max-w-[1240px] px-4 py-14 sm:px-6">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <p className="font-display text-[28px] leading-none font-semibold">
              hoon<span className="text-primary">shop</span>
            </p>
            <p className="mt-3 max-w-xs text-[14px] leading-relaxed text-ink-soft">
              한 알 한 알 손으로 꿴 비즈 액세서리.
              <br />
              주문을 받은 뒤 만들기 시작합니다.
            </p>
            <div className="mt-5 flex gap-2">
              <a
                href="https://instagram.com"
                target="_blank"
                rel="noreferrer noopener"
                aria-label="인스타그램 (새 창)"
                className="flex size-11 items-center justify-center rounded-full border border-line text-ink transition-colors hover:border-ink"
              >
                <InstagramGlyph width={18} height={18} />
              </a>
              <a
                href="https://pf.kakao.com"
                target="_blank"
                rel="noreferrer noopener"
                aria-label="카카오톡 채널 (새 창)"
                className="flex size-11 items-center justify-center rounded-full border border-line text-ink transition-colors hover:border-ink"
              >
                <MessageCircle size={18} strokeWidth={1.6} aria-hidden="true" />
              </a>
            </div>
          </div>

          <nav aria-labelledby="footer-shop">
            <h2 id="footer-shop" className="eyebrow mb-4">
              Shop
            </h2>
            <ul className="space-y-2.5">
              {CATEGORIES.filter((c) => c.id !== 'all').map((c) => (
                <li key={c.id}>
                  <Link
                    to={`/products?category=${c.id}`}
                    className="text-[14px] text-ink-soft transition-colors hover:text-ink"
                  >
                    {c.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <nav aria-labelledby="footer-help">
            <h2 id="footer-help" className="eyebrow mb-4">
              Help
            </h2>
            <ul className="space-y-2.5">
              {HELP.map(([label, to]) => (
                <li key={to}>
                  <Link to={to} className="text-[14px] text-ink-soft transition-colors hover:text-ink">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="mt-12 border-t border-line pt-7 text-[12px] leading-relaxed text-ink-faint">
          <p>훈샵 · 대표 김태훈 · 사업자등록번호 000-00-00000</p>
          <p className="mt-1">서울특별시 중구 세종대로 110 · 고객센터 1234-5678 (평일 10:00–17:00)</p>
          <p className="mt-3">
            © {new Date().getFullYear()} hoonshop. 포트폴리오 목적의 데모이며 실제 거래는
            이루어지지 않습니다.
          </p>
        </div>
      </div>
    </footer>
  );
}
