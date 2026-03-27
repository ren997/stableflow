import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Row,
  Space,
  Spin,
  Tag,
  Typography,
  message
} from 'antd';
import { ArrowLeftOutlined, CheckCircleOutlined, WalletOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../services/http';
import {
  getMerchantPaymentConfig,
  saveMerchantPaymentConfig,
  type MerchantPaymentConfig,
  type MerchantPaymentConfigRequest
} from '../../services/merchantPaymentConfig';
import { clearSession } from '../../services/session';

const DEFAULT_MINT_ADDRESS = 'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v';
const DEFAULT_CHAIN = 'SOLANA';
const PAYMENT_CONFIG_NOT_FOUND = 40402;

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }

  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return value;
  }

  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(timestamp);
}

function CurrentConfigCard({ config }: { config: MerchantPaymentConfig | null }) {
  if (!config) {
    return (
      <Card className="glass-card payment-config-current-card">
        <Empty
          description="No active payment config yet. Save a wallet address to start creating invoices."
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </Card>
    );
  }

  return (
    <Card
      className="glass-card payment-config-current-card"
      title="Current payment config"
      extra={<Tag color={config.activeFlag ? 'success' : 'default'}>{config.activeFlag ? 'Active' : 'Inactive'}</Tag>}
    >
      <Descriptions column={1} size="small" labelStyle={{ width: 120 }}>
        <Descriptions.Item label="Wallet address">{config.walletAddress}</Descriptions.Item>
        <Descriptions.Item label="Mint address">{config.mintAddress}</Descriptions.Item>
        <Descriptions.Item label="Chain">{config.chain}</Descriptions.Item>
        <Descriptions.Item label="Updated at">{formatDateTime(config.updatedAt)}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

export function PaymentConfigPage() {
  const [form] = Form.useForm<MerchantPaymentConfigRequest>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const configQuery = useQuery({
    queryKey: ['merchant-payment-config'],
    queryFn: getMerchantPaymentConfig,
    retry: false
  });

  const configError = configQuery.error instanceof ApiError ? configQuery.error : null;
  const hasNoConfig = configError?.code === PAYMENT_CONFIG_NOT_FOUND;

  const saveMutation = useMutation({
    mutationFn: saveMerchantPaymentConfig,
    onSuccess: (data) => {
      queryClient.setQueryData(['merchant-payment-config'], data);
      message.success('Payment config saved');
    }
  });

  useEffect(() => {
    const error = configQuery.error ?? saveMutation.error;
    if (error instanceof ApiError && error.status === 401) {
      clearSession();
      message.error('Session expired. Please sign in again.');
      navigate('/login', { replace: true });
    }
  }, [configQuery.error, navigate, saveMutation.error]);

  useEffect(() => {
    if (configQuery.data) {
      form.setFieldsValue({
        walletAddress: configQuery.data.walletAddress,
        mintAddress: configQuery.data.mintAddress,
        chain: configQuery.data.chain
      });
      return;
    }

    if (hasNoConfig) {
      form.setFieldsValue({
        walletAddress: '',
        mintAddress: DEFAULT_MINT_ADDRESS,
        chain: DEFAULT_CHAIN
      });
    }
  }, [configQuery.data, form, hasNoConfig]);

  return (
    <div className="dashboard-shell">
      <div className="dashboard-hero">
        <div>
          <Typography.Text className="eyebrow">Merchant settings</Typography.Text>
          <Typography.Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>
            Fixed receiving address
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Save the Solana USDC address that StableFlow should use for payment requests, scanning, and reconciliation.
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dashboard')}>
            Back to dashboard
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={14}>
          <Card className="glass-card payment-config-form-card">
            <Space direction="vertical" size={8} style={{ width: '100%', marginBottom: 24 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                Payment configuration
              </Typography.Title>
              <Typography.Text type="secondary">
                The saved address snapshot will be used when generating future invoice payment requests.
              </Typography.Text>
            </Space>

            <div className="notice-bar payment-config-notice">
              <CheckCircleOutlined /> One active config per merchant. Updating it will affect new invoices only.
            </div>

            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              initialValues={{
                walletAddress: '',
                mintAddress: DEFAULT_MINT_ADDRESS,
                chain: DEFAULT_CHAIN
              }}
              onFinish={(values) => {
                saveMutation.mutate({
                  walletAddress: values.walletAddress.trim(),
                  mintAddress: values.mintAddress.trim(),
                  chain: values.chain.trim().toUpperCase()
                });
              }}
            >
              <Form.Item
                label="Wallet address"
                name="walletAddress"
                extra="Use the merchant's fixed receiving address on Solana."
                rules={[
                  { required: true, message: 'Please enter the wallet address' },
                  { max: 128, message: 'Wallet address must be within 128 characters' }
                ]}
              >
                <Input
                  size="large"
                  placeholder="7xKXtg2CW5ywQ2RkW9sQn8dM8pQ6eG3fQY1Qe6mVnW7K"
                  prefix={<WalletOutlined />}
                />
              </Form.Item>

              <Form.Item
                label="Mint address"
                name="mintAddress"
                extra="Defaulted to Solana mainnet USDC mint. Replace it only if your environment uses a different mint."
                rules={[
                  { required: true, message: 'Please enter the mint address' },
                  { max: 128, message: 'Mint address must be within 128 characters' }
                ]}
              >
                <Input size="large" placeholder={DEFAULT_MINT_ADDRESS} />
              </Form.Item>

              <Form.Item
                label="Chain"
                name="chain"
                extra="StableFlow MVP currently uses the fixed address + reference strategy on Solana."
                rules={[
                  { required: true, message: 'Please enter the chain name' },
                  { max: 32, message: 'Chain must be within 32 characters' }
                ]}
              >
                <Input size="large" placeholder={DEFAULT_CHAIN} />
              </Form.Item>

              <Space wrap>
                <Button type="primary" htmlType="submit" size="large" loading={saveMutation.isPending}>
                  Save config
                </Button>
                <Button
                  size="large"
                  onClick={() => {
                    form.resetFields();
                    if (configQuery.data) {
                      form.setFieldsValue({
                        walletAddress: configQuery.data.walletAddress,
                        mintAddress: configQuery.data.mintAddress,
                        chain: configQuery.data.chain
                      });
                    }
                  }}
                >
                  Reset
                </Button>
              </Space>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={10}>
          {configQuery.isLoading ? (
            <Card className="glass-card payment-config-current-card">
              <div className="center-empty">
                <Spin />
              </div>
            </Card>
          ) : (
            <CurrentConfigCard config={configQuery.data ?? null} />
          )}

          <Card className="glass-card payment-config-tip-card">
            <Typography.Title level={5}>What this config powers</Typography.Title>
            <Space direction="vertical" size={12} className="feature-list">
              <div className="feature-pill">Invoice payment request snapshots</div>
              <div className="feature-pill">Solana transaction scanning</div>
              <div className="feature-pill">Reference-based payment verification</div>
            </Space>
          </Card>
        </Col>
      </Row>

      {configError && !hasNoConfig && configError.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{configError.message}</Typography.Text>
        </Card>
      ) : null}

      {saveMutation.error instanceof ApiError && saveMutation.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{saveMutation.error.message}</Typography.Text>
        </Card>
      ) : null}
    </div>
  );
}
