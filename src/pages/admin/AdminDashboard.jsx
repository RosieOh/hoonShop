import { Link } from 'react-router-dom';
import { AlertTriangle, MessageCircleQuestion, PackageX, Receipt } from 'lucide-react';
import RevenueChart from '@/features/admin/RevenueChart';
import OrderFunnel from '@/features/admin/OrderFunnel';
import { useGetStatsQuery } from '@/features/admin/adminApi';
import { formatPrice } from '@/utils/format';

export default function AdminDashboard() {
  const { data, isLoading, isFetching } = useGetStatsQuery();
  const stale = isFetching && !isLoading;

  return (
    <div className="mx-auto max-w-[1120px]">
      <header className="mb-6">
        <p className="eyebrow">Overview</p>
        <h1 className="mt-2 text-[26px] leading-none font-semibold">대시보드</h1>
      </header>

      {/* 히어로 지표 — 한 화면에 하나만 둡니다 */}
      <section
        aria-label="오늘 실적"
        className="rounded-md border border-line bg-surface p-6 sm:p-7"
      >
        <p className="text-[13px] text-ink-soft">오늘 매출</p>
        {isLoading ? (
          <div className="hs-skeleton mt-2 h-12 w-56 rounded-sm" />
        ) : (
          <p className="mt-1.5 text-[48px] leading-none font-semibold tracking-[-0.02em]">
            {formatPrice(data.todayRevenue)}
            <span className="ml-1 text-[22px] font-medium">원</span>
          </p>
        )}
        <p className="mt-3 text-[13px] text-ink-soft">
          오늘 주문 <b className="font-semibold text-ink">{data?.todayOrders ?? 0}건</b> · 평균
          주문금액{' '}
          <b className="font-semibold text-ink">
            {formatPrice(data?.averageOrderValue ?? 0)}원
          </b>{' '}
          · 누적 주문 <b className="font-semibold text-ink">{data?.orderTotal ?? 0}건</b>
        </p>
      </section>

      {/* 처리해야 할 일 — 숫자보다 "지금 뭘 해야 하는가"가 먼저입니다 */}
      <section aria-label="처리 대기" className="mt-3 grid gap-3 sm:grid-cols-3">
        <ActionTile
          to="/admin/orders?status=PAID"
          icon={Receipt}
          label="처리 대기 주문"
          value={data?.needsAction}
          hint="결제 완료·제작 중"
          loading={isLoading}
          urgent={(data?.needsAction ?? 0) > 0}
        />
        <ActionTile
          to="/admin/products"
          icon={PackageX}
          label="재고 부족"
          value={data?.lowStockCount}
          hint={`품절 ${data?.soldOutCount ?? 0}종 포함`}
          loading={isLoading}
          urgent={(data?.soldOutCount ?? 0) > 0}
        />
        <ActionTile
          to="/admin/inquiries?open=true"
          icon={MessageCircleQuestion}
          label="미답변 문의"
          value={data?.unansweredQna}
          hint="영업일 1일 내 답변"
          loading={isLoading}
          urgent={(data?.unansweredQna ?? 0) > 0}
        />
      </section>

      <div className="mt-3 grid gap-3 lg:grid-cols-[1.6fr_1fr]">
        <RevenueChart data={data?.dailyRevenue ?? []} loading={isLoading} stale={stale} />
        <OrderFunnel statusCounts={data?.statusCounts ?? []} loading={isLoading} />
      </div>

      <div className="mt-3 grid gap-3 lg:grid-cols-2">
        {/* 판매 상위 */}
        <section
          aria-labelledby="top-products"
          className="rounded-md border border-line bg-surface p-5"
        >
          <h2 id="top-products" className="text-[15px] font-semibold">
            많이 팔린 상품
          </h2>
          {isLoading ? (
            <div className="hs-skeleton mt-4 h-40 w-full rounded-sm" />
          ) : (
            <ol className="mt-4 space-y-3">
              {(data.topProducts ?? []).map((p, i) => (
                <li key={p.name} className="flex items-center gap-3 text-[13px]">
                  <span className="tnum w-4 shrink-0 text-[12px] font-semibold text-ink-faint">
                    {i + 1}
                  </span>
                  <span className="min-w-0 flex-1 truncate">{p.name}</span>
                  <span className="tnum shrink-0 text-ink-soft">{p.quantity}개</span>
                  <span className="tnum w-20 shrink-0 text-right font-semibold">
                    {formatPrice(p.revenue)}원
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>

        {/* 재고 경고 */}
        <section
          aria-labelledby="low-stock"
          className="rounded-md border border-line bg-surface p-5"
        >
          <h2 id="low-stock" className="flex items-center gap-2 text-[15px] font-semibold">
            <AlertTriangle size={15} className="text-sale" aria-hidden="true" />
            재고 5개 이하
          </h2>
          {isLoading ? (
            <div className="hs-skeleton mt-4 h-40 w-full rounded-sm" />
          ) : (data.lowStock ?? []).length === 0 ? (
            <p className="mt-4 text-[13px] text-ink-soft">재고가 부족한 상품이 없습니다.</p>
          ) : (
            <ul className="mt-4 space-y-3">
              {data.lowStock.map((p) => (
                <li key={p.id} className="flex items-center gap-3 text-[13px]">
                  <span className="min-w-0 flex-1 truncate">{p.name}</span>
                  <span
                    className={`tnum shrink-0 font-semibold ${p.stock === 0 ? 'text-danger' : 'text-sale'}`}
                  >
                    {p.stock === 0 ? '품절' : `${p.stock}개`}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <Link
            to="/admin/products"
            className="mt-4 flex h-10 items-center justify-center rounded-pill border border-line text-[13px] transition-colors hover:border-ink"
          >
            재고 수정하기
          </Link>
        </section>
      </div>
    </div>
  );
}

/** 처리 대기 타일. 큰 숫자는 비례 숫자(tabular 아님)로 — 표가 아니라 낱개 값입니다. */
function ActionTile({ to, icon: Icon, label, value, hint, loading, urgent }) {
  return (
    <Link
      to={to}
      className="group flex items-center gap-4 rounded-md border border-line bg-surface p-5 transition-colors hover:border-line-strong"
    >
      <span
        className={`flex size-11 shrink-0 items-center justify-center rounded-full ${
          urgent ? 'bg-primary-soft text-primary' : 'bg-canvas text-ink-faint'
        }`}
      >
        <Icon size={19} strokeWidth={1.7} aria-hidden="true" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-[13px] text-ink-soft">{label}</span>
        {loading ? (
          <span className="hs-skeleton mt-1 block h-7 w-12 rounded-sm" />
        ) : (
          <span className="block text-[26px] leading-tight font-semibold">{value ?? 0}</span>
        )}
        <span className="mt-0.5 block text-[12px] text-ink-faint">{hint}</span>
      </span>
    </Link>
  );
}
