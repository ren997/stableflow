import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  WalletOutlined
} from '@ant-design/icons';
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
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../services/http';
import {
  createMerchantWalletOwnershipChallenge,
  getMerchantPaymentConfig,
  saveMerchantPaymentConfig,
  verifyMerchantWalletOwnership,
  type MerchantPaymentConfig,
  type MerchantPaymentConfigRequest,
  type MerchantWalletOwnershipChallenge,
  type MerchantWalletOwnershipStatus,
  type MerchantWalletOwnershipVerifyRequest,
  type MerchantWalletOwnershipVerifyResult
} from '../../services/merchantPaymentConfig';
import { clearSession } from '../../services/session';
import { getSystemRuntimeConfig } from '../../services/system';

const DEFAULT_CHAIN = 'SOLANA';
const PAYMENT_CONFIG_NOT_FOUND = 40402;

function formatDateTime(value?: string | null): string {
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

function getOwnershipStatusMeta(status?: MerchantWalletOwnershipStatus | null): {
  color: string;
  label: string;
  description: string;
} {
  switch (status) {
    case 'CHALLENGE_ISSUED':
      return {
        color: 'processing',
        label: 'Challenge issued',
        description: 'Ask the wallet owner to sign the latest challenge message before it expires.'
      };
    case 'SIGNATURE_SUBMITTED':
      return {
        color: 'gold',
        label: 'Signature submitted',
        description: 'StableFlow saved the signature. The cryptographic verifier can be swapped in later without changing the flow.'
      };
    case 'VERIFIED':
      return {
        color: 'success',
        label: 'Verified',
        description: 'This wallet has already completed the ownership verification flow.'
      };
    case 'FAILED':
      return {
        color: 'error',
        label: 'Verification failed',
        description: 'The last ownership verification attempt failed. Generate a fresh challenge and try again.'
      };
    case 'UNVERIFIED':
    default:
      return {
        color: 'default',
        label: 'Unverified',
        description: 'Generate a challenge to start proving control of the configured receiving address.'
      };
  }
}

function normalizeFormValue(value?: string, uppercase = false): string {
  const normalized = value?.trim() ?? '';
  return uppercase ? normalized.toUpperCase() : normalized;
}

function isConfigDirty(
  config: MerchantPaymentConfig | null | undefined,
  values: Partial<MerchantPaymentConfigRequest> | undefined
): boolean {
  if (!config || !values) {
    return false;
  }

  return (
    normalizeFormValue(values.walletAddress) !== normalizeFormValue(config.walletAddress) ||
    normalizeFormValue(values.mintAddress) !== normalizeFormValue(config.mintAddress) ||
    normalizeFormValue(values.chain, true) !== normalizeFormValue(config.chain, true)
  );
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

  const ownershipMeta = getOwnershipStatusMeta(config.ownershipVerificationStatus);

  return (
    <Card
      className="glass-card payment-config-current-card"
      title="Current payment config"
      extra={<Tag color={config.activeFlag ? 'success' : 'default'}>{config.activeFlag ? 'Active' : 'Inactive'}</Tag>}
    >
      <Descriptions column={1} size="small" labelStyle={{ width: 140 }}>
        <Descriptions.Item label="Wallet address">{config.walletAddress}</Descriptions.Item>
        <Descriptions.Item label="Mint address">{config.mintAddress}</Descriptions.Item>
        <Descriptions.Item label="Chain">{config.chain}</Descriptions.Item>
        <Descriptions.Item label="Ownership status">
          <Space direction="vertical" size={4}>
            <Tag color={ownershipMeta.color}>{ownershipMeta.label}</Tag>
            <Typography.Text type="secondary">{ownershipMeta.description}</Typography.Text>
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="Challenge expires at">
          {formatDateTime(config.ownershipChallengeExpiresAt)}
        </Descriptions.Item>
        <Descriptions.Item label="Signature submitted at">
          {formatDateTime(config.ownershipSignatureSubmittedAt)}
        </Descriptions.Item>
        <Descriptions.Item label="Verified at">{formatDateTime(config.ownershipVerifiedAt)}</Descriptions.Item>
        <Descriptions.Item label="Updated at">{formatDateTime(config.updatedAt)}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

function OwnershipVerificationCard({
  config,
  latestChallenge,
  verificationResult,
  hasUnsavedConfigChanges,
  onCreateChallenge,
  onVerifyOwnership,
  challengeLoading,
  verifyLoading
}: {
  config: MerchantPaymentConfig | null;
  latestChallenge: MerchantWalletOwnershipChallenge | null;
  verificationResult: MerchantWalletOwnershipVerifyResult | null;
  hasUnsavedConfigChanges: boolean;
  onCreateChallenge: () => void;
  onVerifyOwnership: (values: MerchantWalletOwnershipVerifyRequest) => void;
  challengeLoading: boolean;
  verifyLoading: boolean;
}) {
  const [verifyForm] = Form.useForm<MerchantWalletOwnershipVerifyRequest>();
  const ownershipMeta = getOwnershipStatusMeta(config?.ownershipVerificationStatus);

  useEffect(() => {
    verifyForm.setFieldsValue({
      challengeCode: latestChallenge?.challengeCode ?? '',
      signature: ''
    });
  }, [latestChallenge, verifyForm]);

  if (!config) {
    return (
      <Card className="glass-card payment-config-verification-card">
        <Empty
          description="Save the payment config first, then generate a wallet ownership challenge here."
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </Card>
    );
  }

  return (
    <Card
      className="glass-card payment-config-verification-card"
      title="Wallet ownership verification"
      extra={<Tag color={ownershipMeta.color}>{ownershipMeta.label}</Tag>}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          StableFlow keeps wallet ownership as an enhancement layer on top of the fixed address configuration. Generate a
          challenge, sign it in the wallet that controls this address, then submit the signature here.
        </Typography.Paragraph>

        {hasUnsavedConfigChanges ? (
          <div className="notice-bar payment-config-warning">
            <SafetyCertificateOutlined />
            Save the current wallet address first. Changing the address or chain resets ownership verification status.
          </div>
        ) : null}

        <Descriptions column={1} size="small" labelStyle={{ width: 160 }}>
          <Descriptions.Item label="Current status">{ownershipMeta.description}</Descriptions.Item>
          <Descriptions.Item label="Challenge expiry">
            {formatDateTime(latestChallenge?.challengeExpiresAt ?? config.ownershipChallengeExpiresAt)}
          </Descriptions.Item>
          <Descriptions.Item label="Verified at">{formatDateTime(config.ownershipVerifiedAt)}</Descriptions.Item>
        </Descriptions>

        <Space wrap>
          <Button
            type="primary"
            icon={<SafetyCertificateOutlined />}
            onClick={onCreateChallenge}
            loading={challengeLoading}
            disabled={hasUnsavedConfigChanges}
          >
            Generate challenge
          </Button>
        </Space>

        {latestChallenge ? (
          <Card size="small" className="payment-config-inner-card">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Typography.Title level={5} style={{ margin: 0 }}>
                Latest challenge
              </Typography.Title>
              <Typography.Text type="secondary">
                Challenge code {latestChallenge.challengeCode} expires at {formatDateTime(latestChallenge.challengeExpiresAt)}.
              </Typography.Text>
              <Input value={latestChallenge.challengeCode} readOnly />
              <Input.TextArea value={latestChallenge.challengeMessage} autoSize={{ minRows: 6, maxRows: 10 }} readOnly />
            </Space>
          </Card>
        ) : (
          <div className="payment-config-inline-hint">
            Generate a new challenge whenever you need a fresh message to sign. If the message expires, request a new one.
          </div>
        )}

        <Form
          form={verifyForm}
          layout="vertical"
          requiredMark={false}
          onFinish={(values) =>
            onVerifyOwnership({
              challengeCode: values.challengeCode.trim(),
              signature: values.signature.trim()
            })
          }
        >
          <Form.Item
            label="Challenge code"
            name="challengeCode"
            extra="Use the latest challenge code generated for this wallet address."
            rules={[{ required: true, message: 'Please enter the challenge code' }]}
          >
            <Input size="large" placeholder="own_..." />
          </Form.Item>

          <Form.Item
            label="Wallet signature"
            name="signature"
            extra="Paste the signature returned by the wallet after signing the challenge message."
            rules={[
              { required: true, message: 'Please enter the wallet signature' },
              { max: 4096, message: 'Signature must be within 4096 characters' }
            ]}
          >
            <Input.TextArea
              placeholder="Signed payload from the wallet"
              autoSize={{ minRows: 4, maxRows: 8 }}
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" loading={verifyLoading} disabled={hasUnsavedConfigChanges}>
            Submit signature
          </Button>
        </Form>

        {verificationResult ? (
          <Card size="small" className="payment-config-inner-card">
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Typography.Title level={5} style={{ margin: 0 }}>
                Latest verification result
              </Typography.Title>
              <Typography.Text>{verificationResult.verificationMessage}</Typography.Text>
              <Descriptions column={1} size="small" labelStyle={{ width: 180 }}>
                <Descriptions.Item label="Verifier ready">
                  {verificationResult.verifierReady ? 'Enabled' : 'Placeholder mode'}
                </Descriptions.Item>
                <Descriptions.Item label="Submitted at">
                  {formatDateTime(verificationResult.signatureSubmittedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="Verified at">{formatDateTime(verificationResult.verifiedAt)}</Descriptions.Item>
              </Descriptions>
            </Space>
          </Card>
        ) : null}
      </Space>
    </Card>
  );
}

export function PaymentConfigPage() {
  const [form] = Form.useForm<MerchantPaymentConfigRequest>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentFormValues = Form.useWatch([], form) as Partial<MerchantPaymentConfigRequest> | undefined;
  const runtimeConfigQuery = useQuery({
    queryKey: ['system-runtime-config'],
    queryFn: getSystemRuntimeConfig,
    retry: false
  });
  const defaultMintAddress = runtimeConfigQuery.data?.defaultMintAddress ?? '';

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
      challengeMutation.reset();
      verifyMutation.reset();
      message.success('Payment config saved');
    }
  });

  const challengeMutation = useMutation({
    mutationFn: createMerchantWalletOwnershipChallenge,
    onSuccess: (data) => {
      queryClient.setQueryData<MerchantPaymentConfig | undefined>(['merchant-payment-config'], (current) =>
        current
          ? {
              ...current,
              ownershipVerificationStatus: data.ownershipVerificationStatus,
              ownershipChallengeExpiresAt: data.challengeExpiresAt
            }
          : current
      );
      verifyMutation.reset();
      message.success('Ownership challenge generated');
    }
  });

  const verifyMutation = useMutation({
    mutationFn: verifyMerchantWalletOwnership,
    onSuccess: (data) => {
      queryClient.setQueryData<MerchantPaymentConfig | undefined>(['merchant-payment-config'], (current) =>
        current
          ? {
              ...current,
              ownershipVerificationStatus: data.ownershipVerificationStatus,
              ownershipChallengeExpiresAt: data.challengeExpiresAt ?? current.ownershipChallengeExpiresAt,
              ownershipSignatureSubmittedAt: data.signatureSubmittedAt ?? current.ownershipSignatureSubmittedAt,
              ownershipVerifiedAt: data.verifiedAt ?? null
            }
          : current
      );
      message.success('Wallet signature submitted');
    }
  });

  const authError = [
    runtimeConfigQuery.error,
    configQuery.error,
    saveMutation.error,
    challengeMutation.error,
    verifyMutation.error
  ].find((error): error is ApiError => error instanceof ApiError && error.status === 401);

  const hasUnsavedConfigChanges = isConfigDirty(configQuery.data, currentFormValues);

  useEffect(() => {
    if (!authError) {
      return;
    }

    clearSession();
    message.error('Session expired. Please sign in again.');
    navigate('/login', { replace: true });
  }, [authError, navigate]);

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
      if (form.isFieldsTouched()) {
        return;
      }
      form.setFieldsValue({
        walletAddress: '',
        mintAddress: defaultMintAddress,
        chain: DEFAULT_CHAIN
      });
    }
  }, [configQuery.data, defaultMintAddress, form, hasNoConfig]);

  const visibleErrors = [
    runtimeConfigQuery.error instanceof ApiError && runtimeConfigQuery.error.status !== 401 ? runtimeConfigQuery.error : null,
    configError && !hasNoConfig && configError.status !== 401 ? configError : null,
    saveMutation.error instanceof ApiError && saveMutation.error.status !== 401 ? saveMutation.error : null,
    challengeMutation.error instanceof ApiError && challengeMutation.error.status !== 401 ? challengeMutation.error : null,
    verifyMutation.error instanceof ApiError && verifyMutation.error.status !== 401 ? verifyMutation.error : null
  ].filter((error): error is ApiError => Boolean(error));

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
          <Button type="primary" onClick={() => navigate('/invoices')}>
            Invoices
          </Button>
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

            {hasUnsavedConfigChanges ? (
              <div className="notice-bar payment-config-warning">
                <SafetyCertificateOutlined /> Saving a new wallet address or chain resets ownership verification and requires a
                fresh challenge.
              </div>
            ) : null}

            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              initialValues={{
                walletAddress: '',
                mintAddress: '',
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
                extra="Defaults to the current backend Solana network USDC mint when no merchant config exists yet."
                rules={[
                  { required: true, message: 'Please enter the mint address' },
                  { max: 128, message: 'Mint address must be within 128 characters' }
                ]}
              >
                <Input size="large" placeholder={defaultMintAddress || 'USDC mint address'} />
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
                    } else if (hasNoConfig) {
                      form.setFieldsValue({
                        walletAddress: '',
                        mintAddress: defaultMintAddress,
                        chain: DEFAULT_CHAIN
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

          <OwnershipVerificationCard
            config={configQuery.data ?? null}
            latestChallenge={challengeMutation.data ?? null}
            verificationResult={verifyMutation.data ?? null}
            hasUnsavedConfigChanges={hasUnsavedConfigChanges}
            onCreateChallenge={() => challengeMutation.mutate()}
            onVerifyOwnership={(values) => verifyMutation.mutate(values)}
            challengeLoading={challengeMutation.isPending}
            verifyLoading={verifyMutation.isPending}
          />

          <Card className="glass-card payment-config-tip-card">
            <Typography.Title level={5}>What this config powers</Typography.Title>
            <Space direction="vertical" size={12} className="feature-list">
              <div className="feature-pill">Invoice payment request snapshots</div>
              <div className="feature-pill">Solana transaction scanning</div>
              <div className="feature-pill">Reference-based payment verification</div>
              <div className="feature-pill">Merchant wallet ownership workflow</div>
            </Space>
          </Card>
        </Col>
      </Row>

      {visibleErrors.map((error) => (
        <Card className="error-card" key={`${error.status}-${error.code ?? 'no-code'}-${error.message}`}>
          <Typography.Text type="danger">{error.message}</Typography.Text>
        </Card>
      ))}
    </div>
  );
}
