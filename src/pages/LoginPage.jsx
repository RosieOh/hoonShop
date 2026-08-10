import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import LoginView from '@/features/auth/LoginView';

export default function LoginPage() {
  const isAuthed = useSelector((s) => s.auth.isAuthenticated);
  if (isAuthed) return <Navigate to="/mypage" replace />;

  return (
    <div className="mx-auto flex max-w-[1240px] justify-center px-4 py-20 sm:px-6">
      <LoginView />
    </div>
  );
}
