import { useEffect, useState } from 'react';
import { countdown } from '@/utils/format';

/** 타임세일 남은 시간. 종료되면 타이머를 스스로 정리합니다. */
export function useCountdown(targetIso) {
  const [time, setTime] = useState(() => countdown(targetIso));

  useEffect(() => {
    if (!targetIso) return undefined;
    setTime(countdown(targetIso));

    const id = setInterval(() => {
      const next = countdown(targetIso);
      setTime(next);
      if (next.done) clearInterval(id);
    }, 1000);

    return () => clearInterval(id);
  }, [targetIso]);

  return time;
}
