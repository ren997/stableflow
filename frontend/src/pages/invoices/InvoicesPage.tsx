import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Row,
  QRCode,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../../services/http';
import {
  activateInvoice,
  cancelInvoice,
  createInvoice,
  getInvoiceDetail,
  getInvoicePaymentInfo,
  getInvoicePaymentProof,
  getInvoicePaymentStatus,
  listInvoices,
  type CreateInvoiceRequest,
  type InvoiceDetail,
  type InvoicePaymentInfo,
  type InvoicePaymentProof,
  type InvoicePaymentStatus,
  type InvoiceListItem
} from '../../services/invoice';
import { getMerchantPaymentConfig } from '../../services/merchantPaymentConfig';
import { clearSession } from '../../services/session';
import { getSystemRuntimeConfig, type SolanaNetwork } from '../../services/system';

const PAYMENT_CONFIG_NOT_FOUND = 40402;
const PAYMENT_PROOF_NOT_FOUND = 40404;
const DEFAULT_CHAIN = 'SOLANA';
const DEFAULT_CURRENCY = 'USDC';

const statusMeta: Record<string, { label: string; color: string }> = {
  DRAFT: { label: 'Draft', color: 'default' },
  PENDING: { label: 'Pending', color: 'processing' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
  PAID: { label: 'Paid', color: 'success' },
  PARTIALLY_PAID: { label: 'Partial', color: 'gold' },
  OVERPAID: { label: 'Overpaid', color: 'volcano' },
  EXPIRED: { label: 'Expired', color: 'warning' },
  FAILED_RECONCILIATION: { label: 'Failed', color: 'red' }
};

const statusOptions = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'PAID', label: 'Paid' },
  { value: 'PARTIALLY_PAID', label: 'Partial' },
  { value: 'OVERPAID', label: 'Overpaid' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'FAILED_RECONCILIATION', label: 'Failed' }
];

interface InvoiceFormValues {
  customerName: string;
  amount: number;
  currency: string;
  chain: string;
  description?: string;
  expireAt: Dayjs;
}

function formatAmount(value: string | number): string {
  const amount = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(amount)) {
    return String(value);
  }

  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6
  }).format(amount);
}

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

function buildExplorerTxUrl(explorerTxBaseUrl: string, txHash: string, solanaNetwork: SolanaNetwork): string {
  const clusterSuffix = solanaNetwork === 'DEVNET' ? '?cluster=devnet' : '';
  return `${explorerTxBaseUrl}${txHash}${clusterSuffix}`;
}

function InvoiceStatusTag({ status }: { status: string }) {
  const meta = statusMeta[status] ?? { label: status, color: 'default' };
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

function copyText(value: string, successMessage: string) {
  navigator.clipboard.writeText(value).then(() => {
    message.success(successMessage);
  }).catch(() => {
    message.error('Copy failed');
  });
}

function PaymentInfoCard({
  invoice,
  paymentInfo,
  paymentInfoLoading,
  paymentStatus,
  paymentStatusLoading,
  onActivate,
  activateLoading
}: {
  invoice: InvoiceDetail;
  paymentInfo: InvoicePaymentInfo | null;
  paymentInfoLoading: boolean;
  paymentStatus: InvoicePaymentStatus | null;
  paymentStatusLoading: boolean;
  onActivate: () => void;
  activateLoading: boolean;
}) {
  if (invoice.status === 'DRAFT') {
    return (
      <Card
        className="glass-card payment-info-card"
        title="Payment setup"
        extra={<InvoiceStatusTag status={invoice.status} />}
      >
        <Alert
          type="info"
          showIcon
          message="Draft invoices must be activated before sharing payment instructions."
          description="Activation moves the invoice into the pending payment flow and exposes the payment link, reference, and QR code."
          action={
            <Button type="primary" loading={activateLoading} onClick={onActivate}>
              Activate invoice
            </Button>
          }
        />
      </Card>
    );
  }

  if (invoice.status === 'CANCELLED') {
    return (
      <Card
        className="glass-card payment-info-card"
        title="Payment setup"
        extra={<InvoiceStatusTag status={invoice.status} />}
      >
        <Alert
          type="warning"
          showIcon
          message="This invoice has been cancelled."
          description="Cancelled invoices are removed from the payment flow and no longer expose payment instructions."
        />
      </Card>
    );
  }

  return (
    <Card
      className="glass-card payment-info-card"
      title="Payment instructions"
      extra={paymentInfoLoading || paymentStatusLoading ? <Spin size="small" /> : <InvoiceStatusTag status={paymentStatus?.status ?? invoice.status} />}
    >
      {paymentInfoLoading ? (
        <div className="center-empty">
          <Spin />
        </div>
      ) : paymentInfo ? (
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={10}>
            <div className="invoice-qr-shell">
              <QRCode value={paymentInfo.paymentLink} size={192} />
            </div>
          </Col>
          <Col xs={24} md={14}>
            <Descriptions column={1} size="small" labelStyle={{ width: 128 }}>
              <Descriptions.Item label="Recipient">
                <Typography.Text copyable={{ text: paymentInfo.recipientAddress }}>
                  {paymentInfo.recipientAddress}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Reference">
                <Typography.Text copyable={{ text: paymentInfo.referenceKey }}>
                  {paymentInfo.referenceKey}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Mint">
                <Typography.Text copyable={{ text: paymentInfo.mintAddress }}>
                  {paymentInfo.mintAddress}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Amount">
                {formatAmount(paymentInfo.expectedAmount)} {invoice.currency}
              </Descriptions.Item>
              <Descriptions.Item label="Label">{paymentInfo.label || '-'}</Descriptions.Item>
              <Descriptions.Item label="Message">{paymentInfo.message || '-'}</Descriptions.Item>
              <Descriptions.Item label="Expires at">{formatDateTime(paymentInfo.expireAt)}</Descriptions.Item>
            </Descriptions>

            <Space wrap style={{ marginTop: 16 }}>
              <Button type="primary" onClick={() => window.location.assign(paymentInfo.paymentLink)}>
                Open wallet link
              </Button>
              <Button onClick={() => copyText(paymentInfo.paymentLink, 'Payment link copied')}>
                Copy payment link
              </Button>
            </Space>
          </Col>
        </Row>
      ) : (
        <Empty description="Payment information is not available yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </Card>
  );
}

function PaymentStatusCard({
  paymentStatus,
  loading
}: {
  paymentStatus: InvoicePaymentStatus | null;
  loading: boolean;
}) {
  return (
    <Card
      className="glass-card payment-status-card"
      title="Live payment status"
      extra={loading ? <Spin size="small" /> : null}
    >
      {loading && !paymentStatus ? (
        <div className="center-empty">
          <Spin />
        </div>
      ) : paymentStatus ? (
        <>
          <Descriptions column={1} size="small" labelStyle={{ width: 144 }}>
            <Descriptions.Item label="Current status">
              <InvoiceStatusTag status={paymentStatus.status} />
            </Descriptions.Item>
            <Descriptions.Item label="Latest verification">
              {paymentStatus.latestVerificationResult || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Latest tx status">
              {paymentStatus.latestPaymentStatus || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Last processed">
              {formatDateTime(paymentStatus.lastProcessedAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Paid at">
              {formatDateTime(paymentStatus.paidAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Latest tx hash">
              {paymentStatus.latestTxHash ? (
                <Typography.Text copyable={{ text: paymentStatus.latestTxHash }}>
                  {paymentStatus.latestTxHash}
                </Typography.Text>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Exception tags">
              {paymentStatus.exceptionTags.length > 0 ? (
                <Space wrap>
                  {paymentStatus.exceptionTags.map((tag) => (
                    <Tag key={tag} color="warning">
                      {tag}
                    </Tag>
                  ))}
                </Space>
              ) : (
                '-'
              )}
            </Descriptions.Item>
          </Descriptions>
          <Typography.Paragraph type="secondary" style={{ marginTop: 16, marginBottom: 0 }}>
            This panel refreshes automatically while the invoice is in the active payment flow.
          </Typography.Paragraph>
        </>
      ) : (
        <Empty description="No payment status snapshot yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </Card>
  );
}

function PaymentProofCard({
  paymentProof,
  loading,
  empty,
  explorerTxBaseUrl,
  solanaNetwork
}: {
  paymentProof: InvoicePaymentProof | null;
  loading: boolean;
  empty: boolean;
  explorerTxBaseUrl?: string | null;
  solanaNetwork?: SolanaNetwork | null;
}) {
  return (
    <Card
      className="glass-card payment-proof-card"
      title="Payment proof"
      extra={loading ? <Spin size="small" /> : null}
    >
      {loading && !paymentProof ? (
        <div className="center-empty">
          <Spin />
        </div>
      ) : empty ? (
        <Empty
          description="No payment proof yet. It will appear after a transaction is verified and reconciled."
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      ) : paymentProof ? (
        <>
          <Descriptions column={1} size="small" labelStyle={{ width: 152 }}>
            <Descriptions.Item label="Tx hash">
              <Typography.Text copyable={{ text: paymentProof.txHash }}>
                {paymentProof.txHash}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Reference">
              <Typography.Text copyable={{ text: paymentProof.referenceKey }}>
                {paymentProof.referenceKey}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Verification">
              {paymentProof.verificationResult || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Final status">
              <InvoiceStatusTag status={paymentProof.finalStatus} />
            </Descriptions.Item>
            <Descriptions.Item label="Reconciliation">
              {paymentProof.reconciliationStatus || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Amount">
              {formatAmount(paymentProof.amount)}
            </Descriptions.Item>
            <Descriptions.Item label="Paid at">
              {formatDateTime(paymentProof.paidAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Payer">
              {paymentProof.payerAddress ? (
                <Typography.Text copyable={{ text: paymentProof.payerAddress }}>
                  {paymentProof.payerAddress}
                </Typography.Text>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Recipient">
              {paymentProof.recipientAddress ? (
                <Typography.Text copyable={{ text: paymentProof.recipientAddress }}>
                  {paymentProof.recipientAddress}
                </Typography.Text>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Mint">
              <Typography.Text copyable={{ text: paymentProof.mintAddress }}>
                {paymentProof.mintAddress}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Exception tags">
              {paymentProof.exceptionTags.length > 0 ? (
                <Space wrap>
                  {paymentProof.exceptionTags.map((tag) => (
                    <Tag key={tag} color="warning">
                      {tag}
                    </Tag>
                  ))}
                </Space>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Result message">
              {paymentProof.resultMessage || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Proof created">
              {formatDateTime(paymentProof.createdAt)}
            </Descriptions.Item>
          </Descriptions>
          <Space wrap style={{ marginTop: 16 }}>
            <Button onClick={() => copyText(paymentProof.txHash, 'Transaction hash copied')}>
              Copy tx hash
            </Button>
            <Button
              type="primary"
              disabled={!explorerTxBaseUrl || !solanaNetwork}
              onClick={() => {
                if (!explorerTxBaseUrl || !solanaNetwork) {
                  return;
                }
                window.open(
                  buildExplorerTxUrl(explorerTxBaseUrl, paymentProof.txHash, solanaNetwork),
                  '_blank',
                  'noopener,noreferrer'
                );
              }}
            >
              Open explorer
            </Button>
          </Space>
        </>
      ) : (
        <Empty description="No payment proof snapshot yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </Card>
  );
}

function InvoiceDetailCard({
  invoice,
  loading,
  onBackToList,
  onCancel,
  cancelLoading
}: {
  invoice: InvoiceDetail | null;
  loading: boolean;
  onBackToList: () => void;
  onCancel: () => void;
  cancelLoading: boolean;
}) {
  if (loading) {
    return (
      <Card className="glass-card invoice-detail-card">
        <div className="center-empty">
          <Spin />
        </div>
      </Card>
    );
  }

  if (!invoice) {
    return (
      <Card className="glass-card invoice-detail-card">
        <Empty
          description="Create an invoice or select one from the list to inspect the current detail snapshot."
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </Card>
    );
  }

  return (
    <Card
      className="glass-card invoice-detail-card"
      title="Selected invoice"
      extra={
        <Space wrap>
          <InvoiceStatusTag status={invoice.status} />
          {(invoice.status === 'DRAFT' || invoice.status === 'PENDING') ? (
            <Button danger size="small" loading={cancelLoading} onClick={onCancel}>
              Cancel invoice
            </Button>
          ) : null}
          <Button size="small" onClick={onBackToList}>
            Clear selection
          </Button>
        </Space>
      }
    >
      <Descriptions column={1} size="small" labelStyle={{ width: 128 }}>
        <Descriptions.Item label="Invoice no">{invoice.invoiceNo}</Descriptions.Item>
        <Descriptions.Item label="Public id">{invoice.publicId}</Descriptions.Item>
        <Descriptions.Item label="Customer">{invoice.customerName}</Descriptions.Item>
        <Descriptions.Item label="Amount">
          {formatAmount(invoice.amount)} {invoice.currency}
        </Descriptions.Item>
        <Descriptions.Item label="Chain">{invoice.chain}</Descriptions.Item>
        <Descriptions.Item label="Expires at">{formatDateTime(invoice.expireAt)}</Descriptions.Item>
        <Descriptions.Item label="Created at">{formatDateTime(invoice.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="Paid at">{formatDateTime(invoice.paidAt)}</Descriptions.Item>
        <Descriptions.Item label="Description">{invoice.description || '-'}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

export function InvoicesPage() {
  const [createForm] = Form.useForm<InvoiceFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams<{ invoiceId?: string }>();
  const selectedInvoiceId = params.invoiceId ? Number(params.invoiceId) : null;
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const runtimeConfigQuery = useQuery({
    queryKey: ['system-runtime-config'],
    queryFn: getSystemRuntimeConfig,
    retry: false
  });

  const paymentConfigQuery = useQuery({
    queryKey: ['merchant-payment-config'],
    queryFn: getMerchantPaymentConfig,
    retry: false
  });

  const listQuery = useQuery({
    queryKey: ['invoice-list', statusFilter, page, pageSize],
    queryFn: () =>
      listInvoices({
        status: statusFilter || undefined,
        page,
        size: pageSize
      }),
    retry: false
  });

  const detailQuery = useQuery({
    queryKey: ['invoice-detail', selectedInvoiceId],
    queryFn: () => getInvoiceDetail(selectedInvoiceId as number),
    enabled: selectedInvoiceId != null && !Number.isNaN(selectedInvoiceId),
    retry: false
  });

  const paymentInfoQuery = useQuery({
    queryKey: ['invoice-payment-info', selectedInvoiceId],
    queryFn: () => getInvoicePaymentInfo(selectedInvoiceId as number),
    enabled:
      selectedInvoiceId != null
      && selectedInvoiceId > 0
      && detailQuery.data?.status !== 'DRAFT'
      && detailQuery.data?.status !== 'CANCELLED',
    retry: false
  });

  const paymentStatusQuery = useQuery({
    queryKey: ['invoice-payment-status', selectedInvoiceId],
    queryFn: () => getInvoicePaymentStatus(selectedInvoiceId as number),
    enabled: selectedInvoiceId != null && selectedInvoiceId > 0,
    retry: false,
    refetchInterval:
      selectedInvoiceId != null
      && selectedInvoiceId > 0
      && detailQuery.data?.status !== 'DRAFT'
      && detailQuery.data?.status !== 'CANCELLED'
        ? 10000
        : false
  });

  const paymentProofQuery = useQuery({
    queryKey: ['invoice-payment-proof', selectedInvoiceId],
    queryFn: () => getInvoicePaymentProof(selectedInvoiceId as number),
    enabled:
      selectedInvoiceId != null
      && selectedInvoiceId > 0
      && detailQuery.data?.status !== 'DRAFT'
      && detailQuery.data?.status !== 'CANCELLED',
    retry: false,
    refetchInterval:
      selectedInvoiceId != null
      && selectedInvoiceId > 0
      && detailQuery.data?.status !== 'DRAFT'
      && detailQuery.data?.status !== 'CANCELLED'
        ? 10000
        : false
  });

  const createMutation = useMutation({
    mutationFn: createInvoice,
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({ queryKey: ['invoice-list'] });
      queryClient.setQueryData(['invoice-detail', invoice.id], invoice);
      createForm.resetFields();
      createForm.setFieldsValue({
        currency: DEFAULT_CURRENCY,
        chain: DEFAULT_CHAIN,
        expireAt: dayjs().add(1, 'day')
      });
      message.success('Invoice created');
      navigate(`/invoices/${invoice.id}`);
    }
  });

  const activateMutation = useMutation({
    mutationFn: activateInvoice,
    onSuccess: (invoice) => {
      queryClient.setQueryData(['invoice-detail', invoice.id], invoice);
      queryClient.invalidateQueries({ queryKey: ['invoice-list'] });
      queryClient.invalidateQueries({ queryKey: ['invoice-payment-info', invoice.id] });
      queryClient.invalidateQueries({ queryKey: ['invoice-payment-status', invoice.id] });
      message.success('Invoice activated');
    }
  });

  const cancelMutation = useMutation({
    mutationFn: cancelInvoice,
    onSuccess: (invoice) => {
      queryClient.setQueryData(['invoice-detail', invoice.id], invoice);
      queryClient.invalidateQueries({ queryKey: ['invoice-list'] });
      queryClient.invalidateQueries({ queryKey: ['invoice-payment-status', invoice.id] });
      queryClient.removeQueries({ queryKey: ['invoice-payment-info', invoice.id] });
      queryClient.removeQueries({ queryKey: ['invoice-payment-proof', invoice.id] });
      message.success('Invoice cancelled');
    }
  });

  const paymentConfigError = paymentConfigQuery.error instanceof ApiError ? paymentConfigQuery.error : null;
  const missingPaymentConfig = paymentConfigError?.code === PAYMENT_CONFIG_NOT_FOUND;

  useEffect(() => {
    const errors = [
      paymentConfigQuery.error,
      runtimeConfigQuery.error,
      listQuery.error,
      detailQuery.error,
      paymentInfoQuery.error,
      paymentProofQuery.error,
      paymentStatusQuery.error,
      createMutation.error,
      activateMutation.error,
      cancelMutation.error
    ];
    const unauthorized = errors.find((error) => error instanceof ApiError && error.status === 401);
    if (unauthorized instanceof ApiError) {
      clearSession();
      message.error('Session expired. Please sign in again.');
      navigate('/login', { replace: true });
    }
  }, [
    activateMutation.error,
    cancelMutation.error,
    createMutation.error,
    detailQuery.error,
    listQuery.error,
    navigate,
    paymentConfigQuery.error,
    runtimeConfigQuery.error,
    paymentInfoQuery.error,
    paymentProofQuery.error,
    paymentStatusQuery.error
  ]);

  useEffect(() => {
    createForm.setFieldsValue({
      currency: DEFAULT_CURRENCY,
      chain: DEFAULT_CHAIN,
      expireAt: dayjs().add(1, 'day')
    });
  }, [createForm]);

  const tableData = listQuery.data?.records ?? [];
  const selectedInvoice = detailQuery.data
    ? {
        ...detailQuery.data,
        status: paymentStatusQuery.data?.status ?? detailQuery.data.status,
        paidAt: paymentStatusQuery.data?.paidAt ?? detailQuery.data.paidAt
      }
    : null;
  const paymentProofError = paymentProofQuery.error instanceof ApiError ? paymentProofQuery.error : null;
  const missingPaymentProof = paymentProofError?.code === PAYMENT_PROOF_NOT_FOUND;

  const listColumns = useMemo(
    () => [
      {
        title: 'Invoice',
        key: 'invoice',
        render: (_value: unknown, record: InvoiceListItem) => (
          <div>
            <Typography.Text strong>{record.invoiceNo}</Typography.Text>
            <div>
              <Typography.Text type="secondary">{record.customerName}</Typography.Text>
            </div>
          </div>
        )
      },
      {
        title: 'Amount',
        key: 'amount',
        render: (_value: unknown, record: InvoiceListItem) => `${formatAmount(record.amount)} ${record.currency}`
      },
      {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
        render: (status: string) => <InvoiceStatusTag status={status} />
      },
      {
        title: 'Expires',
        dataIndex: 'expireAt',
        key: 'expireAt',
        render: (value: string) => formatDateTime(value)
      },
      {
        title: 'Created',
        dataIndex: 'createdAt',
        key: 'createdAt',
        render: (value: string) => formatDateTime(value)
      },
      {
        title: 'Action',
        key: 'action',
        render: (_value: unknown, record: InvoiceListItem) => (
          <Button size="small" onClick={() => navigate(`/invoices/${record.id}`)}>
            View detail
          </Button>
        )
      }
    ],
    [navigate]
  );

  return (
    <div className="dashboard-shell">
      <div className="dashboard-hero">
        <div>
          <Typography.Text className="eyebrow">Invoices</Typography.Text>
          <Typography.Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>
            Create and review merchant invoices
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Use this page to create invoice drafts, inspect current records, and move into the payment flow.
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dashboard')}>
            Back to dashboard
          </Button>
          <Button type="primary" onClick={() => navigate('/settings/payment-config')}>
            Payment config
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={10}>
          <Card className="glass-card invoice-create-card">
            <Space direction="vertical" size={8} style={{ width: '100%', marginBottom: 20 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                Create invoice
              </Typography.Title>
              <Typography.Text type="secondary">
                StableFlow will create a payment request snapshot from the active merchant receiving address.
              </Typography.Text>
            </Space>

            {missingPaymentConfig ? (
              <Alert
                type="warning"
                showIcon
                message="Payment config required"
                description="Set up the merchant receiving address before creating your first invoice."
                action={
                  <Button size="small" type="primary" onClick={() => navigate('/settings/payment-config')}>
                    Configure now
                  </Button>
                }
                className="invoice-alert"
              />
            ) : null}

            {paymentConfigQuery.data ? (
              <div className="notice-bar invoice-config-notice">
                Active destination: {paymentConfigQuery.data.chain} · {paymentConfigQuery.data.walletAddress}
              </div>
            ) : null}

            <Form<InvoiceFormValues>
              form={createForm}
              layout="vertical"
              requiredMark={false}
              onFinish={(values) => {
                const payload: CreateInvoiceRequest = {
                  customerName: values.customerName.trim(),
                  amount: values.amount,
                  currency: values.currency.trim().toUpperCase(),
                  chain: values.chain.trim().toUpperCase(),
                  description: values.description?.trim() || undefined,
                  expireAt: values.expireAt.toDate().toISOString()
                };
                createMutation.mutate(payload);
              }}
            >
              <Form.Item
                label="Customer name"
                name="customerName"
                rules={[
                  { required: true, message: 'Please enter the customer name' },
                  { max: 128, message: 'Customer name must be within 128 characters' }
                ]}
              >
                <Input size="large" placeholder="Alice" disabled={missingPaymentConfig} />
              </Form.Item>

              <Row gutter={12}>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Amount"
                    name="amount"
                    rules={[
                      { required: true, message: 'Please enter the invoice amount' },
                      {
                        validator: async (_rule, value: number | undefined) => {
                          if (typeof value !== 'number' || value <= 0) {
                            throw new Error('Amount must be greater than 0');
                          }
                        }
                      }
                    ]}
                  >
                    <InputNumber
                      size="large"
                      min={0.000001}
                      step={0.01}
                      precision={6}
                      style={{ width: '100%' }}
                      placeholder="99.00"
                      disabled={missingPaymentConfig}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Currency"
                    name="currency"
                    rules={[
                      { required: true, message: 'Please enter the currency' },
                      { max: 16, message: 'Currency must be within 16 characters' }
                    ]}
                  >
                    <Input size="large" placeholder={DEFAULT_CURRENCY} disabled={missingPaymentConfig} />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={12}>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Chain"
                    name="chain"
                    rules={[
                      { required: true, message: 'Please enter the chain name' },
                      { max: 32, message: 'Chain must be within 32 characters' }
                    ]}
                  >
                    <Input size="large" placeholder={DEFAULT_CHAIN} disabled={missingPaymentConfig} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Expire at"
                    name="expireAt"
                    rules={[{ required: true, message: 'Please choose the invoice expiry time' }]}
                  >
                    <DatePicker
                      size="large"
                      showTime
                      style={{ width: '100%' }}
                      format="YYYY-MM-DD HH:mm"
                      disabled={missingPaymentConfig}
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="Description"
                name="description"
                rules={[{ max: 512, message: 'Description must be within 512 characters' }]}
              >
                <Input.TextArea
                  rows={4}
                  placeholder="Monthly subscription fee"
                  showCount
                  maxLength={512}
                  disabled={missingPaymentConfig}
                />
              </Form.Item>

              <Space wrap>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<PlusOutlined />}
                  size="large"
                  loading={createMutation.isPending}
                  disabled={missingPaymentConfig}
                >
                  Create invoice
                </Button>
                <Button
                  size="large"
                  onClick={() =>
                    createForm.setFieldsValue({
                      customerName: '',
                      amount: undefined,
                      currency: DEFAULT_CURRENCY,
                      chain: DEFAULT_CHAIN,
                      description: '',
                      expireAt: dayjs().add(1, 'day')
                    })
                  }
                >
                  Reset
                </Button>
              </Space>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <InvoiceDetailCard
              invoice={selectedInvoice}
              loading={detailQuery.isLoading}
              onBackToList={() => navigate('/invoices')}
              onCancel={() => {
                if (selectedInvoiceId) {
                  cancelMutation.mutate(selectedInvoiceId);
                }
              }}
              cancelLoading={cancelMutation.isPending}
            />

            {selectedInvoice ? (
              <>
                <PaymentInfoCard
                  invoice={selectedInvoice}
                  paymentInfo={paymentInfoQuery.data ?? selectedInvoice.paymentInfo ?? null}
                  paymentInfoLoading={paymentInfoQuery.isLoading || activateMutation.isPending}
                  paymentStatus={paymentStatusQuery.data ?? null}
                  paymentStatusLoading={paymentStatusQuery.isLoading || paymentStatusQuery.isFetching}
                  onActivate={() => {
                    if (selectedInvoiceId) {
                      activateMutation.mutate(selectedInvoiceId);
                    }
                  }}
                  activateLoading={activateMutation.isPending}
                />
                <PaymentStatusCard
                  paymentStatus={paymentStatusQuery.data ?? null}
                  loading={paymentStatusQuery.isLoading || paymentStatusQuery.isFetching}
                />
                <PaymentProofCard
                  paymentProof={paymentProofQuery.data ?? null}
                  loading={paymentProofQuery.isLoading || paymentProofQuery.isFetching}
                  empty={missingPaymentProof}
                  explorerTxBaseUrl={runtimeConfigQuery.data?.explorerTxBaseUrl}
                  solanaNetwork={runtimeConfigQuery.data?.solanaNetwork}
                />
              </>
            ) : null}

            <Card
              className="glass-card invoice-table-card"
              title="Invoice list"
              extra={
                <Space wrap className="invoice-filter-bar">
                  <Select
                    value={statusFilter}
                    options={statusOptions}
                    style={{ minWidth: 180 }}
                    onChange={(value) => {
                      setStatusFilter(value);
                      setPage(1);
                    }}
                  />
                  <Button onClick={() => listQuery.refetch()} loading={listQuery.isFetching}>
                    Refresh
                  </Button>
                </Space>
              }
            >
              <Table<InvoiceListItem>
                rowKey="id"
                columns={listColumns}
                dataSource={tableData}
                loading={listQuery.isLoading}
                pagination={{
                  current: listQuery.data?.page ?? page,
                  pageSize: listQuery.data?.size ?? pageSize,
                  total: listQuery.data?.total ?? 0,
                  onChange: (nextPage, nextPageSize) => {
                    setPage(nextPage);
                    setPageSize(nextPageSize);
                  }
                }}
                locale={{
                  emptyText: <Empty description="No invoices yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                }}
                onRow={(record) => ({
                  onClick: () => navigate(`/invoices/${record.id}`)
                })}
              />
            </Card>
          </Space>
        </Col>
      </Row>

      {paymentConfigError && !missingPaymentConfig && paymentConfigError.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{paymentConfigError.message}</Typography.Text>
        </Card>
      ) : null}

      {runtimeConfigQuery.error instanceof ApiError && runtimeConfigQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{runtimeConfigQuery.error.message}</Typography.Text>
        </Card>
      ) : null}

      {listQuery.error instanceof ApiError && listQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{listQuery.error.message}</Typography.Text>
        </Card>
      ) : null}

      {detailQuery.error instanceof ApiError && detailQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{detailQuery.error.message}</Typography.Text>
        </Card>
      ) : null}

      {paymentInfoQuery.error instanceof ApiError && paymentInfoQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{paymentInfoQuery.error.message}</Typography.Text>
        </Card>
      ) : null}

      {paymentStatusQuery.error instanceof ApiError && paymentStatusQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{paymentStatusQuery.error.message}</Typography.Text>
        </Card>
      ) : null}

      {paymentProofError && !missingPaymentProof && paymentProofError.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{paymentProofError.message}</Typography.Text>
        </Card>
      ) : null}

      {createMutation.error instanceof ApiError && createMutation.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{createMutation.error.message}</Typography.Text>
        </Card>
      ) : null}

      {activateMutation.error instanceof ApiError && activateMutation.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{activateMutation.error.message}</Typography.Text>
        </Card>
      ) : null}

      {cancelMutation.error instanceof ApiError && cancelMutation.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{cancelMutation.error.message}</Typography.Text>
        </Card>
      ) : null}
    </div>
  );
}
