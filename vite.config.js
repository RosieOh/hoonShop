import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import fs from 'node:fs'

/**
 * GitHub Pages는 https://<user>.github.io/<repo>/ 하위에서 서빙되므로
 * 프로덕션 빌드에만 서브경로를 붙입니다. 개발 서버는 그대로 '/'를 씁니다.
 */
const REPO_BASE = '/hoonShop/'

/**
 * SPA 폴백.
 * Pages는 서버 라우팅이 없어 /hoonShop/products 로 직접 들어오면 404를 냅니다.
 * 404.html을 index.html과 동일하게 두면 GitHub이 이 파일을 대신 내려주고,
 * 그때부터는 React Router가 경로를 처리합니다.
 */
function spaFallback() {
  return {
    name: 'spa-fallback-404',
    apply: 'build',
    closeBundle() {
      const dist = path.resolve(import.meta.dirname, 'dist')
      const index = path.join(dist, 'index.html')
      if (fs.existsSync(index)) {
        fs.copyFileSync(index, path.join(dist, '404.html'))
        // Jekyll이 _로 시작하는 파일을 걸러내지 않도록
        fs.writeFileSync(path.join(dist, '.nojekyll'), '')
      }
    },
  }
}

// https://vite.dev/config/
// mode 기준으로 판단합니다. command로 나누면 `vite preview`(command: 'serve')가
// 프로덕션 산출물을 '/'에서 서빙해 에셋 경로가 어긋납니다.
export default defineConfig(({ mode }) => ({
  base: mode === 'production' ? REPO_BASE : '/',
  plugins: [react(), tailwindcss(), spaFallback()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
}))
