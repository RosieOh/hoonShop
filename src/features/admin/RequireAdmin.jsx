import { Navigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { ShieldAlert } from 'lucide-react';
import Button from '@/components/common/Button';

/**
 * 관리자 전용 구간 가드.
 *
 * 비로그인과 "로그인은 했지만 권한이 없음"은 다른 상황이라 다르게 다룹니다.
 *  · 비로그인 → 로그인 페이지로 보내고, 끝나면 원래 가려던 곳으로 돌려보냅니다.
 *  · 권한 없음 → 로그인 페이지로 보내면 무한 루프가 되므로, 이유를 설명하고 멈춥니다.
 *
 * ⚠️ 프론트 가드는 UX 장치일 뿐 보안 장치가 아닙니다.
 *    실제 차단은 서버가 토큰의 role을 검사해서 해야 합니다.
 */
export default function RequireAdmin({ children }) {
  const location = useLocation();
  const isAuthed = useSelector((s) => s.auth.isAuthenticated);
  const role = useSelector((s) => s.auth.user?.role);

  if (!isAuthed) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (role !== 'admin') {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center px-4 py-24 text-center">
        <span className="flex size-16 items-center justify-center rounded-full bg-primary-soft text-primary">
          <ShieldAlert size={26} strokeWidth={1.5} aria-hidden="true" />
        </span>
        <h1 className="font-display mt-6 text-[30px] leading-tight font-semibold">
          접근 권한이 없어요
        </h1>
        <p className="mt-3 text-[14px] leading-relaxed text-ink-soft">
          관리자 계정으로 로그인해야 볼 수 있는 페이지입니다.
          <br />
          데모 관리자 계정: <b className="text-ink">admin@hoonshop.com</b> / <b className="text-ink">hoonshop</b>
        </p>
        <div className="mt-8 flex gap-2">
          <Button to="/" variant="outline">
            홈으로
          </Button>
          <Button to="/mypage">계정 전환</Button>
        </div>
      </div>
    );
  }

  return children;
}
