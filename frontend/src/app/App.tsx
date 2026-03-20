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
      <AppRouter />
    </ConfigProvider>
  );
}
