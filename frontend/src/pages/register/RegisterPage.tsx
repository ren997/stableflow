import { useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Button, Card, Col, Form, Input, Row, Space, Typography, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { register, type RegisterRequest } from '../../services/auth';
import { getAccessToken, setAuthenticatedMerchantSession } from '../../services/session';

const registerBullets = ['Create your merchant workspace', 'Get instant dashboard access', 'Start billing before chain traffic lands'];

/**
 * Merchant registration page with direct dashboard entry after success.
 */
export function RegisterPage() {
  const [form] = Form.useForm<RegisterRequest>();
  const navigate = useNavigate();
  const registerMutation = useMutation({
    mutationFn: register,
    onSuccess: (data) => {
      setAuthenticatedMerchantSession(data);
      message.success('Account created');
      navigate('/dashboard', { replace: true });
    }
  });

  useEffect(() => {
    if (getAccessToken()) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate]);

  return (
    <div className="auth-shell">
      <Row className="auth-grid" align="middle" gutter={[32, 32]}>
        <Col xs={24} lg={12}>
          <div className="auth-hero">
            <Typography.Text className="eyebrow">StableFlow onboarding</Typography.Text>
            <Typography.Title level={1}>Set up your merchant account and land on the dashboard already signed in.</Typography.Title>
            <Typography.Paragraph>
              Create a workspace for invoice operations, payment verification, and reconciliation without a separate onboarding tool.
            </Typography.Paragraph>
            <Space direction="vertical" size={12} className="feature-list">
              {registerBullets.map((bullet) => (
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
              Create merchant account
            </Typography.Title>
            <Typography.Paragraph type="secondary">
              Register once and continue directly into the StableFlow dashboard.
            </Typography.Paragraph>
            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              onFinish={(values) => {
                registerMutation.mutate(values);
              }}
            >
              <Form.Item
                label="Merchant name"
                name="merchantName"
                rules={[
                  { required: true, message: 'Please enter your merchant name' },
                  { max: 100, message: 'Merchant name must be 100 characters or fewer' }
                ]}
              >
                <Input size="large" placeholder="StableFlow Demo Store" autoComplete="organization" />
              </Form.Item>
              <Form.Item
                label="Email"
                name="email"
                rules={[
                  { required: true, message: 'Please enter your email' },
                  { type: 'email', message: 'Please enter a valid email' }
                ]}
              >
                <Input size="large" placeholder="merchant@stableflow.com" autoComplete="email" />
              </Form.Item>
              <Form.Item
                label="Password"
                name="password"
                rules={[
                  { required: true, message: 'Please enter your password' },
                  { min: 6, message: 'Password must be at least 6 characters' }
                ]}
              >
                <Input.Password size="large" placeholder="Create a password" autoComplete="new-password" />
              </Form.Item>
              <Button type="primary" htmlType="submit" size="large" block loading={registerMutation.isPending}>
                Create account
              </Button>
            </Form>
            <div className="auth-switch">
              <Typography.Text type="secondary">Already have a merchant account?</Typography.Text>
              <Link to="/login">Sign in</Link>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
