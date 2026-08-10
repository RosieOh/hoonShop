import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { KeyRound } from 'lucide-react';
import Button from '@/components/common/Button';
import Field from '@/components/common/Field';
import { useToast } from '@/components/common/Toast';
import { useLoginMutation } from './authApi';
import { setRedirectTo } from './authSlice';
import { validateForm, validators } from '@/utils/validate';

export default function LoginView() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const { toast } = useToast();
  const [login, { isLoading }] = useLoginMutation();

  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);

  const from = location.state?.from ?? '/';

  const submit = async (e) => {
    e.preventDefault();
    const nextErrors = validateForm(values, ['email', 'password']);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    try {
      await login(values).unwrap();
      dispatch(setRedirectTo(null));
      toast('로그인되었어요');
      navigate(from, { replace: true });
    } catch (err) {
      setServerError(err?.data?.message ?? '로그인 중 문제가 발생했습니다.');
    }
  };

  const fillDemo = () => {
    setValues({ email: 'hoon@example.com', password: 'hoonshop' });
    setErrors({});
    setServerError(null);
  };

  return (
    <div className="mx-auto w-full max-w-sm">
      <h1 className="font-display text-[36px] leading-tight font-semibold">다시 만나 반가워요</h1>
      <p className="mt-2 text-[14px] text-ink-soft">
        주문 내역과 쿠폰은 로그인 후 확인할 수 있어요.
      </p>

      <form onSubmit={submit} noValidate className="mt-8 space-y-4">
        <Field
          label="이메일"
          type="email"
          required
          autoComplete="email"
          value={values.email}
          onChange={(e) => setValues({ ...values, email: e.target.value })}
          onBlur={() => setErrors({ ...errors, email: validators.email(values.email) ?? undefined })}
          error={errors.email}
          placeholder="hoon@example.com"
        />

        <Field
          label="비밀번호"
          type="password"
          required
          autoComplete="current-password"
          value={values.password}
          onChange={(e) => setValues({ ...values, password: e.target.value })}
          onBlur={() =>
            setErrors({ ...errors, password: validators.password(values.password) ?? undefined })
          }
          error={errors.password}
        />

        {serverError && (
          <p role="alert" className="rounded-md bg-[#FDF2F1] px-4 py-3 text-[13px] text-danger">
            {serverError}
          </p>
        )}

        <Button type="submit" full size="lg" loading={isLoading} className="mt-2">
          로그인
        </Button>

        <Button type="button" variant="ghost" full icon={KeyRound} onClick={fillDemo}>
          데모 계정으로 채우기
        </Button>
      </form>

      <p className="mt-6 rounded-md bg-canvas p-4 text-[12px] leading-relaxed text-ink-soft">
        데모 계정 — 이메일 <b className="text-ink">hoon@example.com</b> / 비밀번호{' '}
        <b className="text-ink">hoonshop</b>
      </p>
    </div>
  );
}
