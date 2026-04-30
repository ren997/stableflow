import { ConfigProvider, theme } from 'antd';
import { AppRouter } from '../router';

export default function App() {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#0f766e',
          borderRadius: 14,
          fontFamily: "'Segoe UI', 'PingFang SC', sans-serif"
        }
      }}
    >
      <div className="app-shell">
        <main className="app-main">
          <AppRouter />
        </main>
        <footer className="site-footer">
          <a
            href="https://beian.miit.gov.cn/"
            target="_blank"
            rel="noreferrer"
            className="site-footer-link"
          >
            粤ICP备2026021283号
          </a>
        </footer>
      </div>
    </ConfigProvider>
  );
}
