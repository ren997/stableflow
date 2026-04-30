import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Descriptions, Empty, QRCode, Row, Space, Spin, Tag, Typography } from 'antd';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../services/http';
import { getPublicPaymentPage } from '../../services/publicPayment';

const finalStatuses = new Set(['PAID', 'OVERPAID', 'PARTIALLY_PAID', 'EXPIRED', 'FAILED_RECONCILIATION']);

const statusMeta: Record<string, { label: string; color: string; tone: 'info' | 'success' | 'warning' | 'error' }> = {
  DRAFT: { label: 'Draft', color: 'default', tone: 'info' },
  PENDING: { label: 'Pending', color: 'processing', tone: 'info' },
  CANCELLED: { label: 'Cancelled', color: 'default', tone: 'warning' },
  PAID: { label: 'Paid', color: 'success', tone: 'success' },
  PARTIALLY_PAID: { label: 'Partial', color: 'gold', tone: 'warning' },
  OVERPAID: { label: 'Overpaid', color: 'volcano', tone: 'warning' },
  EXPIRED: { label: 'Expired', color: 'warning', tone: 'warning' },
  FAILED_RECONCILIATION: { label: 'Failed', color: 'red', tone: 'error' }
};

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

function copyText(value: string, messageText: string) {
  navigator.clipboard.writeText(value).then(() => {
    window.alert(messageText);
  }).catch(() => {
    window.alert('Copy failed');
  });
}

export function PublicPaymentPage() {
  const params = useParams<{ publicId?: string }>();
  const publicId = params.publicId ?? '';

  const paymentPageQuery = useQuery({
    queryKey: ['public-payment-page', publicId],
    queryFn: () => getPublicPaymentPage(publicId),
    enabled: publicId.length > 0,
    retry: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status) {
        return 10000;
      }
      return finalStatuses.has(status) ? false : 10000;
    }
  });

  const error = paymentPageQuery.error instanceof ApiError ? paymentPageQuery.error : null;
  const page = paymentPageQuery.data;
  const meta = useMemo(() => {
    if (!page) {
      return null;
    }
    return statusMeta[page.status] ?? { label: page.status, color: 'default', tone: 'info' as const };
  }, [page]);

  return (
    <div className="public-payment-shell">
      <div className="public-payment-backdrop" />
      <div className="public-payment-content">
        <div className="public-payment-header">
          <div>
            <Typography.Text className="eyebrow">StableFlow payment</Typography.Text>
            <Typography.Title level={1} className="public-payment-title">
              {page ? `${formatAmount(page.amount)} ${page.currency}` : 'Invoice payment'}
            </Typography.Title>
            <Typography.Paragraph type="secondary" className="public-payment-subtitle">
              {page
                ? `Invoice ${page.invoiceNo} for ${page.customerName}. This page refreshes automatically while payment is pending.`
                : 'Open a shareable Solana payment page with QR code and reference-based settlement details.'}
            </Typography.Paragraph>
          </div>
          <Link to="/">
            <Button>Merchant console</Button>
          </Link>
        </div>

        {paymentPageQuery.isLoading ? (
          <Card className="glass-card public-payment-card">
            <div className="center-empty">
              <Spin size="large" />
            </div>
          </Card>
        ) : error ? (
          <Card className="glass-card public-payment-card">
            <Empty
              description={error.code === 40403 ? 'This payment page is unavailable.' : error.message}
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          </Card>
        ) : page && meta ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Alert
              type={meta.tone}
              showIcon
              message={
                page.status === 'PAID'
                  ? 'Payment received'
                  : page.status === 'PENDING'
                    ? 'Waiting for payment'
                    : `Invoice status: ${meta.label}`
              }
              description={
                page.status === 'PAID'
                  ? `Confirmed at ${formatDateTime(page.paidAt)}`
                  : page.status === 'PENDING'
                    ? `Complete the transfer before ${formatDateTime(page.expireAt)}`
                    : page.status === 'EXPIRED'
                      ? 'This invoice is no longer accepting new payment attempts.'
                      : 'You can still review the payment instructions and latest invoice state here.'
              }
            />

            <Row gutter={[16, 16]}>
              <Col xs={24} xl={10}>
                <Card className="glass-card public-payment-card">
                  <div className="public-payment-qr-shell">
                    <QRCode value={page.paymentInfo.paymentLink} size={220} />
                  </div>
                  <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 20 }}>
                    <Button type="primary" size="large" block onClick={() => window.location.assign(page.paymentInfo.paymentLink)}>
                      Open wallet link
                    </Button>
                    <Button block onClick={() => copyText(page.paymentInfo.paymentLink, 'Payment link copied')}>
                      Copy payment link
                    </Button>
                  </Space>
                </Card>
              </Col>

              <Col xs={24} xl={14}>
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  <Card
                    className="glass-card public-payment-card"
                    title="Payment details"
                    extra={<Tag color={meta.color}>{meta.label}</Tag>}
                  >
                    <Descriptions column={1} size="small" labelStyle={{ width: 132 }}>
                      <Descriptions.Item label="Invoice no">{page.invoiceNo}</Descriptions.Item>
                      <Descriptions.Item label="Customer">{page.customerName}</Descriptions.Item>
                      <Descriptions.Item label="Amount">
                        {formatAmount(page.amount)} {page.currency}
                      </Descriptions.Item>
                      <Descriptions.Item label="Chain">{page.chain}</Descriptions.Item>
                      <Descriptions.Item label="Description">{page.description || '-'}</Descriptions.Item>
                      <Descriptions.Item label="Expires at">{formatDateTime(page.expireAt)}</Descriptions.Item>
                      <Descriptions.Item label="Paid at">{formatDateTime(page.paidAt)}</Descriptions.Item>
                    </Descriptions>
                  </Card>

                  <Card className="glass-card public-payment-card" title="Transfer instructions">
                    <Descriptions column={1} size="small" labelStyle={{ width: 132 }}>
                      <Descriptions.Item label="Recipient">
                        <Typography.Text copyable={{ text: page.paymentInfo.recipientAddress }}>
                          {page.paymentInfo.recipientAddress}
                        </Typography.Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Reference">
                        <Typography.Text copyable={{ text: page.paymentInfo.referenceKey }}>
                          {page.paymentInfo.referenceKey}
                        </Typography.Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Mint">
                        <Typography.Text copyable={{ text: page.paymentInfo.mintAddress }}>
                          {page.paymentInfo.mintAddress}
                        </Typography.Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Label">{page.paymentInfo.label || '-'}</Descriptions.Item>
                      <Descriptions.Item label="Message">{page.paymentInfo.message || '-'}</Descriptions.Item>
                    </Descriptions>
                  </Card>
                </Space>
              </Col>
            </Row>
          </Space>
        ) : null}
      </div>
    </div>
  );
}
