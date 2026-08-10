import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { store } from '@/app/store';
import App from '@/app/App';
import { ToastProvider } from '@/components/common/Toast';
import '@/styles/global.css';

/**
 * 백엔드가 아직 없으므로 MSW(Mock Service Worker)가 /api/* 요청을 가로챕니다.
 * 워커가 준비된 뒤 렌더해야 첫 요청부터 목 응답을 받을 수 있습니다.
 */
async function bootstrap() {
  if (import.meta.env.DEV || import.meta.env.VITE_ENABLE_MOCK === 'true') {
    const { startMockServer } = await import('@/mocks/browser');
    await startMockServer();
  }

  // GitHub Pages 서브경로(/hoonShop/) 배포 대응. 로컬 개발에서는 빈 문자열이 됩니다.
  const basename = import.meta.env.BASE_URL.replace(/\/$/, '');

  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <Provider store={store}>
        <BrowserRouter basename={basename}>
          <ToastProvider>
            <App />
          </ToastProvider>
        </BrowserRouter>
      </Provider>
    </StrictMode>,
  );
}

bootstrap();
