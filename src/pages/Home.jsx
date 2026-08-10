import { Link } from 'react-router-dom';
import { ArrowRight, Hand, PackageCheck, Sparkles } from 'lucide-react';
import Button from '@/components/common/Button';
import BeadArt from '@/components/common/BeadArt';
import ProductGrid from '@/features/products/ProductGrid';
import { useGetProductsQuery } from '@/features/products/productApi';
import { useGetPromotionsQuery } from '@/features/marketing/promoApi';
import { useCountdown } from '@/hooks/useCountdown';
import { CATEGORIES } from '@/mocks/db';

const HERO_PRODUCT = {
  id: 'HERO',
  name: 'hero',
  category: 'necklace',
  palette: ['#F6D89B', '#FFF2D6', '#D9B679', '#FBE7BC'],
};

const PROMISES = [
  { icon: Hand, title: '주문 후 제작', body: '재고를 쌓아두지 않고 받은 순서대로 만듭니다.' },
  { icon: Sparkles, title: '무니켈 부자재', body: '금속 알레르기가 있어도 편하게 착용하세요.' },
  { icon: PackageCheck, title: '5만원 이상 무료배송', body: '평일 오후 2시 이전 결제분은 당일 발송.' },
];

export default function Home() {
  const { data: newArrivals, isLoading: loadingNew } = useGetProductsQuery({ sort: 'new', size: 8 });
  const { data: best, isLoading: loadingBest } = useGetProductsQuery({ badge: 'best', size: 4 });
  const { data: promo } = useGetPromotionsQuery();

  const timesale = promo?.items?.find((p) => p.kind === 'timesale');
  const time = useCountdown(timesale?.endsAt);

  return (
    <>
      {/* ---------------------------------------------------------- HERO --- */}
      <section className="mx-auto max-w-[1240px] px-4 pt-10 pb-16 sm:px-6 sm:pt-16">
        <div className="grid items-center gap-10 lg:grid-cols-[1.05fr_1fr]">
          <div className="hs-rise">
            <p className="eyebrow">Handmade beaded jewelry</p>

            {/* Cormorant에는 한글 글리프가 없어 본문 폰트로 대체됩니다.
                이 자리에서 가짜 이탤릭을 쓰면 자모가 뭉개지므로 색으로만 강조합니다. */}
            <h1 className="font-display mt-4 text-[clamp(2.75rem,8vw,5.25rem)] leading-[1.02] font-semibold tracking-[-0.03em]">
              한 알씩,
              <br />
              <span className="text-primary">손으로</span> 꿴 것들
            </h1>

            <p className="mt-6 max-w-md text-[16px] leading-relaxed text-ink-soft">
              공장에서 찍어낸 액세서리 말고요. 오늘 기분에 맞는 색을 고르면, 그때부터 만들기
              시작합니다.
            </p>

            <div className="mt-9 flex flex-wrap gap-2.5">
              <Button to="/products" size="lg" icon={ArrowRight} iconRight>
                전체 상품 보기
              </Button>
              <Button to="/products?sort=new" size="lg" variant="outline">
                이번 주 신상
              </Button>
            </div>

            <dl className="mt-12 flex gap-8 border-t border-line pt-6">
              {[
                ['24', '종의 컬러웨이'],
                ['4.7', '평균 별점'],
                ['3,400+', '누적 주문'],
              ].map(([value, label]) => (
                <div key={label}>
                  <dt className="sr-only">{label}</dt>
                  <dd>
                    <span className="tnum font-display block text-[28px] leading-none font-semibold">
                      {value}
                    </span>
                    <span className="mt-1.5 block text-[12px] text-ink-soft">{label}</span>
                  </dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="media-frame hs-fade aspect-[4/5] rounded-lg sm:aspect-square lg:aspect-[4/5]">
            <BeadArt product={HERO_PRODUCT} decorative className="h-full w-full" />
            <p className="font-label absolute bottom-5 left-5 rounded-pill bg-surface/85 px-4 py-2 text-[11px] font-semibold tracking-[0.14em] backdrop-blur-sm">
              MADE TO ORDER
            </p>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------ 카테고리 --- */}
      <section aria-labelledby="cat-heading" className="mx-auto max-w-[1240px] px-4 sm:px-6">
        <h2 id="cat-heading" className="sr-only">
          카테고리 바로가기
        </h2>
        <ul className="scrollbar-none -mx-4 flex gap-3 overflow-x-auto px-4 pb-2 sm:mx-0 sm:grid sm:grid-cols-3 sm:px-0 lg:grid-cols-6">
          {CATEGORIES.filter((c) => c.id !== 'all').map((c, i) => (
            <li key={c.id} className="shrink-0">
              <Link
                to={`/products?category=${c.id}`}
                className="hs-rise group flex w-32 flex-col items-center gap-3 rounded-md border border-line bg-surface p-4 transition-colors hover:border-line-strong sm:w-auto"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <span className="media-frame size-16 rounded-full">
                  <BeadArt
                    product={{ id: `cat-${c.id}`, name: c.label, category: c.id, palette: undefined }}
                    decorative
                    className="h-full w-full transition-transform duration-300 group-hover:scale-110"
                  />
                </span>
                <span className="text-[13px] font-medium">{c.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      {/* -------------------------------------------------------- 타임세일 --- */}
      {timesale && !time.done && (
        <section
          aria-labelledby="timesale-heading"
          className="mx-auto mt-20 max-w-[1240px] px-4 sm:px-6"
        >
          <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-4">
            <div>
              <p className="eyebrow text-sale">Today only</p>
              <h2 id="timesale-heading" className="font-display mt-2 text-[32px] leading-none font-semibold">
                {timesale.title}
              </h2>
            </div>
            <p className="tnum font-label flex items-center gap-1.5 text-[15px] font-semibold">
              <span className="rounded-sm bg-ink px-2 py-1.5 text-canvas">{time.hours}</span>
              <span aria-hidden="true">:</span>
              <span className="rounded-sm bg-ink px-2 py-1.5 text-canvas">{time.minutes}</span>
              <span aria-hidden="true">:</span>
              <span className="rounded-sm bg-ink px-2 py-1.5 text-canvas">{time.seconds}</span>
              <span className="sr-only">남음</span>
            </p>
          </div>

          <div className="mt-8">
            <ProductGrid products={best?.items ?? []} loading={loadingBest} skeletonCount={4} />
          </div>
        </section>
      )}

      {/* ---------------------------------------------------------- 신상 --- */}
      <section aria-labelledby="new-heading" className="mx-auto mt-20 max-w-[1240px] px-4 sm:px-6">
        <div className="flex items-end justify-between gap-4 border-b border-line pb-4">
          <div>
            <p className="eyebrow">New arrivals</p>
            <h2 id="new-heading" className="font-display mt-2 text-[32px] leading-none font-semibold">
              이번 주에 나온 것들
            </h2>
          </div>
          <Link
            to="/products?sort=new"
            className="flex h-11 shrink-0 items-center gap-1 text-[14px] text-ink-soft transition-colors hover:text-ink"
          >
            더 보기
            <ArrowRight size={15} aria-hidden="true" />
          </Link>
        </div>

        <div className="mt-8">
          <ProductGrid products={newArrivals?.items ?? []} loading={loadingNew} />
        </div>
      </section>

      {/* --------------------------------------------------------- 약속 --- */}
      <section aria-labelledby="promise-heading" className="mt-24 bg-surface py-16">
        <div className="mx-auto max-w-[1240px] px-4 sm:px-6">
          <h2 id="promise-heading" className="font-display text-[28px] font-semibold">
            훈샵이 지키는 것
          </h2>
          <ul className="mt-8 grid gap-8 sm:grid-cols-3">
            {PROMISES.map(({ icon: Icon, title, body }) => (
              <li key={title}>
                <span className="flex size-11 items-center justify-center rounded-full bg-primary-soft text-primary">
                  <Icon size={19} strokeWidth={1.6} aria-hidden="true" />
                </span>
                <h3 className="mt-4 text-[16px] font-semibold">{title}</h3>
                <p className="mt-1.5 text-[14px] leading-relaxed text-ink-soft">{body}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </>
  );
}
