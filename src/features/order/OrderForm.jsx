import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { MapPin, Plus } from 'lucide-react';
import Field from '@/components/common/Field';
import { Skeleton } from '@/components/common/Skeleton';
import { useGetAddressesQuery } from './orderApi';
import { selectAddress, setDeliveryMemo, startNewAddress, updateShippingField } from './orderSlice';
import { validators } from '@/utils/validate';
import { cn, formatPhone } from '@/utils/format';

const MEMOS = [
  '문 앞에 놓아주세요',
  '부재 시 경비실에 맡겨주세요',
  '배송 전 연락 부탁드려요',
  '파손 위험이 있으니 조심히 다뤄주세요',
  '직접 입력',
];

const REQUIRED = ['recipient', 'phone', 'zipcode', 'address1'];

/**
 * 배송지 입력.
 * 검증은 blur 시점에 실행합니다 — 타이핑 중에 빨간 글씨가 뜨면
 * 아직 다 적지도 않았는데 혼나는 느낌이 듭니다.
 */
export default function OrderForm({ errors, onErrorsChange }) {
  const dispatch = useDispatch();
  const { data, isLoading } = useGetAddressesQuery();
  const selectedId = useSelector((s) => s.order.selectedAddressId);
  const address = useSelector((s) => s.order.shippingAddress);
  const memo = useSelector((s) => s.order.deliveryMemo);

  const [memoPreset, setMemoPreset] = useState(MEMOS[0]);

  // 최초 진입 시 기본 배송지를 자동 선택
  useEffect(() => {
    if (!selectedId && data?.items?.length) {
      dispatch(selectAddress(data.items.find((a) => a.isDefault) ?? data.items[0]));
    }
  }, [data, selectedId, dispatch]);

  const isNew = selectedId === 'new';

  const handleBlur = (field) => {
    const message = validators[field]?.(address[field]);
    onErrorsChange({ ...errors, [field]: message ?? undefined });
  };

  const setField = (field, value) => {
    dispatch(updateShippingField({ field, value }));
    // 이미 에러가 떠 있는 필드는 고치는 즉시 지워줍니다.
    if (errors[field] && !validators[field]?.(value)) {
      onErrorsChange({ ...errors, [field]: undefined });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-20 w-full rounded-md" />
        <Skeleton className="h-20 w-full rounded-md" />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="grid gap-2 sm:grid-cols-2">
        {(data?.items ?? []).map((item) => {
          const active = selectedId === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => {
                dispatch(selectAddress(item));
                onErrorsChange({});
              }}
              aria-pressed={active}
              className={cn(
                'rounded-md border p-4 text-left transition-colors',
                active ? 'border-primary bg-primary-soft' : 'border-line hover:border-line-strong',
              )}
            >
              <span className="flex items-center gap-1.5 text-[13px] font-semibold">
                <MapPin size={13} aria-hidden="true" className="text-primary" />
                {item.label}
                {item.isDefault && (
                  <span className="rounded-sm bg-ink px-1.5 py-0.5 text-[10px] font-medium text-canvas">
                    기본
                  </span>
                )}
              </span>
              <span className="mt-1.5 block text-[13px] text-ink">
                {item.recipient} · {item.phone}
              </span>
              <span className="mt-0.5 block text-[13px] leading-snug text-ink-soft">
                ({item.zipcode}) {item.address1} {item.address2}
              </span>
            </button>
          );
        })}

        <button
          type="button"
          onClick={() => {
            dispatch(startNewAddress());
            onErrorsChange({});
          }}
          aria-pressed={isNew}
          className={cn(
            'flex min-h-24 flex-col items-center justify-center gap-1.5 rounded-md border border-dashed p-4 transition-colors',
            isNew
              ? 'border-primary bg-primary-soft text-primary'
              : 'border-line-strong text-ink-soft hover:border-ink hover:text-ink',
          )}
        >
          <Plus size={18} aria-hidden="true" />
          <span className="text-[13px] font-medium">새 배송지 입력</span>
        </button>
      </div>

      {isNew && (
        <div className="hs-rise grid gap-4 rounded-md border border-line p-5 sm:grid-cols-2">
          <Field
            label="받는 분"
            required
            value={address.recipient}
            onChange={(e) => setField('recipient', e.target.value)}
            onBlur={() => handleBlur('recipient')}
            error={errors.recipient}
            autoComplete="name"
          />
          <Field
            label="연락처"
            required
            inputMode="numeric"
            value={address.phone}
            onChange={(e) => setField('phone', formatPhone(e.target.value))}
            onBlur={() => handleBlur('phone')}
            error={errors.phone}
            placeholder="010-1234-5678"
            autoComplete="tel"
          />
          <Field
            label="우편번호"
            required
            inputMode="numeric"
            value={address.zipcode}
            onChange={(e) => setField('zipcode', e.target.value.replace(/\D/g, '').slice(0, 5))}
            onBlur={() => handleBlur('zipcode')}
            error={errors.zipcode}
            placeholder="04524"
            autoComplete="postal-code"
          />
          <Field
            label="기본 주소"
            required
            className="sm:col-span-2"
            value={address.address1}
            onChange={(e) => setField('address1', e.target.value)}
            onBlur={() => handleBlur('address1')}
            error={errors.address1}
            autoComplete="address-line1"
          />
          <Field
            label="상세 주소"
            className="sm:col-span-2"
            value={address.address2}
            onChange={(e) => setField('address2', e.target.value)}
            hint="동·호수까지 적어주시면 배송이 빨라져요"
            autoComplete="address-line2"
          />
        </div>
      )}

      <div className="space-y-2">
        <Field
          as="select"
          label="배송 메모"
          value={memoPreset}
          onChange={(e) => {
            setMemoPreset(e.target.value);
            dispatch(setDeliveryMemo(e.target.value === '직접 입력' ? '' : e.target.value));
          }}
        >
          {MEMOS.map((m) => (
            <option key={m} value={m}>
              {m}
            </option>
          ))}
        </Field>

        {memoPreset === '직접 입력' && (
          <Field
            label="메모 직접 입력"
            className="hs-fade"
            value={memo}
            onChange={(e) => dispatch(setDeliveryMemo(e.target.value))}
            maxLength={50}
            hint={`${memo.length}/50`}
          />
        )}
      </div>
    </div>
  );
}

export { REQUIRED as REQUIRED_ADDRESS_FIELDS };
