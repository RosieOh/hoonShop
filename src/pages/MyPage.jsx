import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ChevronRight, Heart, LayoutDashboard, LogOut, Package, TicketPercent } from 'lucide-react';
import Button from '@/components/common/Button';
import EmptyState from '@/components/common/EmptyState';
import LoginView from '@/features/auth/LoginView';
import { logout } from '@/features/auth/authSlice';
import { useGetOrdersQuery } from '@/features/order/orderApi';
import { useGetCouponsQuery } from '@/features/marketing/promoApi';
import { isExpired } from '@/features/marketing/couponRules';
import { useToast } from '@/components/common/Toast';
import { formatDate, formatPrice } from '@/utils/format';

export default function MyPage() {
  const dispatch = useDispatch();
  const { toast } = useToast();
  const user = useSelector((s) => s.auth.user);
  const isAuthed = useSelector((s) => s.auth.isAuthenticated);
  const wishCount = useSelector((s) => s.wishlist.ids.length);

  const { data: orderData } = useGetOrdersQuery(undefined, { skip: !isAuthed });
  const { data: couponData } = useGetCouponsQuery(undefined, { skip: !isAuthed });

  if (!isAuthed) {
    return (
      <div className="mx-auto flex max-w-[1240px] justify-center px-4 py-20 sm:px-6">
        <LoginView />
      </div>
    );
  }

  const orders = orderData?.items ?? [];
  const usableCoupons = (couponData?.items ?? []).filter((c) => !isExpired(c));

  return (
    <div className="mx-auto max-w-[1240px] px-4 pb-16 sm:px-6">
      <header className="flex flex-wrap items-end justify-between gap-4 pt-8 pb-8">
        <div>
          <p className="eyebrow">{user.grade} member</p>
          <h1 className="font-display mt-2 text-[34px] leading-none font-semibold sm:text-[42px]">
            {user.name}님
          </h1>
          <p className="mt-2 text-[14px] text-ink-soft">{user.email}</p>
        </div>

        <Button
          variant="ghost"
          icon={LogOut}
          onClick={() => {
            dispatch(logout());
            toast('로그아웃되었어요', { tone: 'info' });
          }}
        >
          로그아웃
        </Button>
      </header>

      {user.role === 'admin' && (
        <Link
          to="/admin"
          className="mb-3 flex items-center gap-3 rounded-md border border-primary/25 bg-primary-soft p-5 transition-colors hover:border-primary/50"
        >
          <LayoutDashboard size={20} className="shrink-0 text-primary" aria-hidden="true" />
          <span className="min-w-0 flex-1">
            <span className="block text-[15px] font-semibold text-primary">관리자 콘솔</span>
            <span className="block text-[13px] text-ink-soft">
              주문 처리, 재고 수정, 문의 답변을 여기서 합니다
            </span>
          </span>
          <ChevronRight size={18} className="shrink-0 text-primary" aria-hidden="true" />
        </Link>
      )}

      <ul className="grid grid-cols-3 gap-3">
        <SummaryCard icon={Package} label="주문" value={orders.length} to="#orders" />
        <SummaryCard icon={Heart} label="위시리스트" value={wishCount} to="/wishlist" />
        <SummaryCard icon={TicketPercent} label="쿠폰" value={usableCoupons.length} to="#coupons" />
      </ul>

      <p className="tnum mt-3 rounded-md bg-primary-soft px-5 py-4 text-[14px] text-primary">
        보유 포인트 <b className="font-semibold">{formatPrice(user.point)}P</b> · 다음 등급까지
        30,000원
      </p>

      {/* ---------------------------------------------------------- 주문 --- */}
      <section id="orders" aria-labelledby="orders-heading" className="mt-14 scroll-mt-24">
        <h2 id="orders-heading" className="text-[20px] font-semibold">
          주문 내역
        </h2>

        {orders.length === 0 ? (
          <EmptyState
            icon={Package}
            title="아직 주문 내역이 없어요"
            description="이 데모의 주문 내역은 새로고침하면 초기화됩니다."
            actionLabel="상품 보러 가기"
            actionTo="/products"
          />
        ) : (
          <ul className="mt-5 space-y-3">
            {orders.map((order) => (
              <li key={order.id} className="rounded-md border border-line bg-surface p-5">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-3">
                  <div>
                    <p className="tnum text-[15px] font-semibold">{order.id}</p>
                    <p className="mt-0.5 text-[12px] text-ink-soft">{formatDate(order.createdAt)}</p>
                  </div>
                  <span className="rounded-pill bg-success-soft px-3 py-1.5 text-[12px] font-medium text-success">
                    {order.status === 'PAID' ? '결제 완료' : '결제 대기'}
                  </span>
                </div>

                <ul className="mt-3 space-y-1.5 text-[14px]">
                  {order.items.map((item, i) => (
                    <li key={i} className="flex justify-between gap-4">
                      <span className="min-w-0 truncate">
                        {item.name}
                        <span className="text-ink-soft"> · {item.quantity}개</span>
                      </span>
                      <span className="tnum shrink-0">{formatPrice(item.price * item.quantity)}원</span>
                    </li>
                  ))}
                </ul>

                <p className="mt-3 flex justify-between border-t border-line pt-3 text-[14px]">
                  <span className="text-ink-soft">결제 금액</span>
                  <span className="tnum text-[16px] font-semibold">
                    {formatPrice(order.amount)}원
                  </span>
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* ---------------------------------------------------------- 쿠폰 --- */}
      <section id="coupons" aria-labelledby="coupons-heading" className="mt-14 scroll-mt-24">
        <h2 id="coupons-heading" className="text-[20px] font-semibold">
          보유 쿠폰
        </h2>

        <ul className="mt-5 grid gap-3 sm:grid-cols-2">
          {(couponData?.items ?? []).map((coupon) => {
            const expired = isExpired(coupon);
            return (
              <li
                key={coupon.id}
                className={`flex items-center gap-4 rounded-md border p-5 ${
                  expired ? 'border-line bg-canvas opacity-55' : 'border-line bg-surface'
                }`}
              >
                <span className="font-display grid size-14 shrink-0 place-items-center rounded-sm bg-primary-soft text-[17px] font-semibold text-primary">
                  {coupon.type === 'percent'
                    ? `${coupon.value}%`
                    : coupon.type === 'shipping'
                      ? '배송'
                      : `${coupon.value / 1000}천`}
                </span>
                <span className="min-w-0">
                  <span className="block text-[15px] font-medium">{coupon.name}</span>
                  <span className="mt-0.5 block text-[12px] text-ink-soft">
                    {formatDate(coupon.expiresAt)}까지 {expired && '· 만료됨'}
                  </span>
                </span>
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}

function SummaryCard({ icon: Icon, label, value, to }) {
  return (
    <li>
      <Link
        to={to}
        className="flex flex-col items-center gap-1.5 rounded-md border border-line bg-surface py-5 transition-colors hover:border-line-strong"
      >
        <Icon size={19} strokeWidth={1.6} className="text-primary" aria-hidden="true" />
        <span className="tnum font-display text-[24px] leading-none font-semibold">{value}</span>
        <span className="text-[12px] text-ink-soft">{label}</span>
      </Link>
    </li>
  );
}
