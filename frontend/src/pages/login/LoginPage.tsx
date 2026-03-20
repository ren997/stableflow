import { useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Button, Card, Col, Form, Input, Row, Space, Typography, message } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { login, type LoginRequest } from '../../services/auth';
import { getAccessToken, setAccessToken, setMerchantSession } from '../../services/session';

const loginBullets = ['Fixed address billing', 'Reference-based reconciliation', 'Dashboard-ready workflow'];

export function LoginPage() {
  const [form] = Form.useForm<LoginRequest>();
  const navigate = useNavigate();
  const location = useLocation();
  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setMerchantSession({
        merchantId: data.merchantId,
        merchantName: data.merchantName,
        email: data.email
      });
      message.success('Welcome back');
      navigate('/dashboard', { replace: true });
    }
  });

  useEffect(() => {
    if (getAccessToken()) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate]);

  const from = (location.state as { from?: string } | null)?.from;

  return (
    <div className="auth-shell">
      <Row className="auth-grid" align="middle" gutter={[32, 32]}>
        <Col xs={24} lg={12}>
          <div className="auth-hero">
            <Typography.Text className="eyebrow">StableFlow merchant console</Typography.Text>
            <Typography.Title level={1}>Login once, track every invoice from chain to proof.</Typography.Title>
            <Typography.Paragraph>
              A focused dashboard for Solana stablecoin billing, payment verification, and reconciliation.
            </Typography.Paragraph>
            <Space direction="vertical" size={12} className="feature-list">
              {loginBullets.map((bullet) => (
                <div key={bullet} className="feature-pill">
                  {bullet}
                </div>
              ))}
            </Space>
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <Card className="glass-card auth-card">
            <Typography.Title level={3} style={{ marginTop: 0 }}>
              Merchant Login
            </Typography.Title>
            <Typography.Paragraph type="secondary">
              Sign in to access your invoices, payment status, and dashboard summary.
            </Typography.Paragraph>
            {from ? (
              <div className="notice-bar">Please sign in to continue to {from}.</div>
            ) : null}
            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              onFinish={(values) => {
                loginMutation.mutate(values);
              }}
            >
              <Form.Item
                label="Email"
                name="email"
                rules={[
                  { required: true, message: 'Please enter your email' },
                  { type: 'email', message: 'Please enter a valid email' }
                ]}
              >
                <Input size="large" placeholder="demo@stableflow.com" autoComplete="email" />
              </Form.Item>
              <Form.Item
                label="Password"
                name="password"
                rules={[{ required: true, message: 'Please enter your password' }]}
              >
                <Input.Password size="large" placeholder="Password" autoComplete="current-password" />
              </Form.Item>
              <Button type="primary" htmlType="submit" size="large" block loading={loginMutation.isPending}>
                Sign in
              </Button>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
