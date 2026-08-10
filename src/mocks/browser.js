import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

/**
 * 실제 백엔드가 준비되면 main.jsx의 이 호출만 제거하면 됩니다.
 * 핸들러에 없는 요청은 그대로 네트워크로 통과시킵니다(bypass).
 */
export function startMockServer() {
  return worker.start({
    onUnhandledRequest: 'bypass',
    quiet: true,
    serviceWorker: { url: `${import.meta.env.BASE_URL}mockServiceWorker.js` },
  });
}
