import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { CreditCard, ShieldCheck, TriangleAlert } from 'lucide-react';
import Field from '@/components/common/Field';
import { PAYMENT_METHODS, setAllAgreements, setMethod, toggleAgreement } from './paymentSlice';
import { validators } from '@/utils/validate';
import { cn, formatCardNumber } from '@/utils/format';

/**
 * 결제 수단 선택 + 카드 정보 입력.
 *
 * ⚠️ 실서비스에서는 카드번호를 절대 우리 서버로 보내지 않습니다.
 *    PG사 SDK(토스페이먼츠/포트원)가 발급한 결제창에서 입력받고,
 *    프론트는 결과로 받은 paymentKey만 서버에 전달합니다.
 *    아래 입력 폼은 흐름 시연용 UI입니다.
 */
export default function PaymentModule({ onCardChange }) {
  const dispatch = useDispatch();
  const method = useSelector((s) => s.payment.method);
  const agreements = useSelector((s) => s.payment.agreements);
  const error = useSelector((s) => s.payment.error);

  const [card, setCard] = useState({ number: '', expiry: '', cvc: '' });
  const [errors, setErrors] = useState({});

  const update = (field, value) => {
    const next = { ...card, [field]: value };
    setCard(next);
    onCardChange?.(next);
  };

  const blur = (field, validatorKey) =>
    setErrors((prev) => ({ ...prev, [field]: validators[validatorKey](card[field]) ?? undefined }));

  const allAgreed = agreements.terms && agreements.privacy;

  return (
    <div className="space-y-6">
      <fieldset>
        <legend className="mb-3 text-[15px] font-semibold">결제 수단</legend>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {PAYMENT_METHODS.map((m) => (
            <button
              key={m.id}
              type="button"
              onClick={() => dispatch(setMethod(m.id))}
              aria-pressed={method === m.id}
              className={cn(
                'h-12 rounded-md border text-[14px] transition-colors',
                method === m.id
                  ? 'border-primary bg-primary-soft font-medium text-primary'
                  : 'border-line text-ink-soft hover:border-line-strong hover:text-ink',
              )}
            >
              {m.label}
            </button>
          ))}
        </div>
      </fieldset>

      {method === 'CARD' && (
        <div className="hs-fade space-y-4 rounded-md border border-line p-5">
          <p className="flex items-center gap-1.5 text-[12px] text-ink-soft">
            <ShieldCheck size={14} className="text-success" aria-hidden="true" />
            카드 정보는 PG사로 직접 전송되며 훈샵 서버에 저장되지 않습니다.
          </p>

          <Field
            label="카드번호"
            required
            inputMode="numeric"
            autoComplete="cc-number"
            value={card.number}
            onChange={(e) => update('number', formatCardNumber(e.target.value))}
            onBlur={() => blur('number', 'cardNumber')}
            error={errors.number}
            placeholder="0000 0000 0000 0000"
          />

          <div className="grid grid-cols-2 gap-3">
            <Field
              label="유효기간"
              required
              inputMode="numeric"
              autoComplete="cc-exp"
              value={card.expiry}
              onChange={(e) => {
                const digits = e.target.value.replace(/\D/g, '').slice(0, 4);
                update('expiry', digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits);
              }}
              onBlur={() => blur('expiry', 'cardExpiry')}
              error={errors.expiry}
              placeholder="MM/YY"
            />
            <Field
              label="CVC"
              required
              inputMode="numeric"
              autoComplete="cc-csc"
              type="password"
              value={card.cvc}
              onChange={(e) => update('cvc', e.target.value.replace(/\D/g, '').slice(0, 3))}
              onBlur={() => blur('cvc', 'cardCvc')}
              error={errors.cvc}
              placeholder="뒷면 3자리"
            />
          </div>

          <p className="flex items-start gap-1.5 rounded-sm bg-canvas p-3 text-[12px] text-ink-soft">
            <CreditCard size={13} className="mt-0.5 shrink-0" aria-hidden="true" />
            테스트: 아무 번호나 입력하면 승인됩니다. 끝 4자리가 <b>0000</b>이면 승인 거절 화면을
            볼 수 있어요.
          </p>
        </div>
      )}

      {method !== 'CARD' && (
        <p className="hs-fade rounded-md bg-canvas p-4 text-[13px] text-ink-soft">
          결제하기를 누르면 {PAYMENT_METHODS.find((m) => m.id === method)?.label} 창으로
          이동합니다.
        </p>
      )}

      {error && (
        <div
          role="alert"
          className="flex items-start gap-2 rounded-md border border-danger/30 bg-[#FDF2F1] p-4 text-[13px] text-danger"
        >
          <TriangleAlert size={15} className="mt-0.5 shrink-0" aria-hidden="true" />
          <span>
            <b className="block font-semibold">결제가 완료되지 않았어요</b>
            {error.message}
          </span>
        </div>
      )}

      <fieldset className="rounded-md border border-line">
        <legend className="sr-only">약관 동의</legend>

        <label className="flex h-13 cursor-pointer items-center gap-3 border-b border-line px-4">
          <input
            type="checkbox"
            checked={allAgreed}
            onChange={(e) => dispatch(setAllAgreements(e.target.checked))}
            className="size-5 accent-[#7A3B52]"
          />
          <span className="text-[15px] font-semibold">전체 동의</span>
        </label>

        <label className="flex h-12 cursor-pointer items-center gap-3 px-4">
          <input
            type="checkbox"
            checked={agreements.terms}
            onChange={() => dispatch(toggleAgreement('terms'))}
            className="size-5 accent-[#7A3B52]"
          />
          <span className="text-[14px] text-ink-soft">
            <span className="text-sale">(필수)</span> 구매조건 및 결제진행 동의
          </span>
        </label>

        <label className="flex h-12 cursor-pointer items-center gap-3 px-4 pb-1">
          <input
            type="checkbox"
            checked={agreements.privacy}
            onChange={() => dispatch(toggleAgreement('privacy'))}
            className="size-5 accent-[#7A3B52]"
          />
          <span className="text-[14px] text-ink-soft">
            <span className="text-sale">(필수)</span> 개인정보 제3자 제공 동의
          </span>
        </label>
      </fieldset>
    </div>
  );
}
