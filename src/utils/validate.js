const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_RE = /^01[016789]-?\d{3,4}-?\d{4}$/;
const ZIP_RE = /^\d{5}$/;

/**
 * 필드별 검증기. 통과하면 null, 실패하면 사용자가 읽을 수 있는 메시지를 반환합니다.
 * "입력값이 올바르지 않습니다" 같은 문구는 쓰지 않습니다 — 무엇을 어떻게 고쳐야 하는지 알려줍니다.
 */
export const validators = {
  email: (v) => {
    if (!v?.trim()) return '이메일을 입력해 주세요.';
    if (!EMAIL_RE.test(v.trim())) return '이메일 형식이 맞지 않아요. 예: hoon@example.com';
    return null;
  },
  password: (v) => {
    if (!v) return '비밀번호를 입력해 주세요.';
    if (v.length < 8) return `8자 이상 입력해 주세요. (현재 ${v.length}자)`;
    return null;
  },
  recipient: (v) => {
    if (!v?.trim()) return '받는 분 이름을 입력해 주세요.';
    if (v.trim().length < 2) return '2자 이상 입력해 주세요.';
    return null;
  },
  phone: (v) => {
    if (!v?.trim()) return '연락처를 입력해 주세요.';
    if (!PHONE_RE.test(v.trim())) return '휴대폰 번호 11자리를 입력해 주세요. 예: 010-1234-5678';
    return null;
  },
  zipcode: (v) => {
    if (!v?.trim()) return '우편번호를 입력해 주세요.';
    if (!ZIP_RE.test(v.trim())) return '우편번호는 숫자 5자리입니다.';
    return null;
  },
  address1: (v) => (v?.trim() ? null : '기본 주소를 입력해 주세요.'),
  address2: () => null,
  cardNumber: (v) => {
    const digits = (v ?? '').replace(/\D/g, '');
    if (!digits) return '카드번호를 입력해 주세요.';
    if (digits.length !== 16) return `카드번호 16자리를 모두 입력해 주세요. (현재 ${digits.length}자)`;
    return null;
  },
  cardExpiry: (v) => {
    if (!/^\d{2}\/\d{2}$/.test(v ?? '')) return 'MM/YY 형식으로 입력해 주세요.';
    const [mm] = v.split('/').map(Number);
    if (mm < 1 || mm > 12) return '월(月)은 01~12 사이여야 합니다.';
    return null;
  },
  cardCvc: (v) => (/^\d{3}$/.test(v ?? '') ? null : 'CVC 3자리를 입력해 주세요.'),
};

/** 폼 전체 검증: { field: message } 형태의 에러 맵을 반환 */
export function validateForm(values, fields) {
  return fields.reduce((errors, field) => {
    const message = validators[field]?.(values[field]);
    if (message) errors[field] = message;
    return errors;
  }, {});
}
