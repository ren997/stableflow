import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Col, Empty, Row, Space, Spin, Statistic, Tag, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
  getDashboardStatusDistribution,
  getDashboardSummary,
  type DashboardStatusCountItem,
} from '../../services/dashboard';
import { ApiError } from '../../services/http';
import { clearSession, getMerchantSession } from '../../services/session';

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

function formatAmount(value: string | number): string {
  const amount = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(amount)) {
    return String(value);
  }

  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount);
}

function StatusCard({ item }: { item: DashboardStatusCountItem }) {
  const meta = statusMeta[item.status] ?? { label: item.status, color: 'default' };

  return (
    <Card className="status-card">
      <div className="status-card-header">
        <Tag color={meta.color}>{meta.label}</Tag>
        <Typography.Text type="secondary">{item.status}</Typography.Text>
      </div>
      <Typography.Title level={2} style={{ margin: 0 }}>
        {item.count}
      </Typography.Title>
    </Card>
  );
}

export function DashboardPage() {
  const navigate = useNavigate();
  const merchant = getMerchantSession();

  const summaryQuery = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: getDashboardSummary,
    retry: false
  });

  const statusQuery = useQuery({
    queryKey: ['dashboard-status-distribution'],
    queryFn: getDashboardStatusDistribution,
    retry: false
  });

  useEffect(() => {
    const error = summaryQuery.error ?? statusQuery.error;
    if (error instanceof ApiError && error.status === 401) {
      clearSession();
      message.error('Session expired. Please sign in again.');
      navigate('/login', { replace: true });
    }
  }, [navigate, statusQuery.error, summaryQuery.error]);

  const logout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  const summary = summaryQuery.data;
  const distribution = statusQuery.data ?? [];

  return (
    <div className="dashboard-shell">
      <div className="dashboard-hero">
        <div>
          <Typography.Text className="eyebrow">Dashboard</Typography.Text>
          <Typography.Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>
            Welcome back{merchant?.merchantName ? `, ${merchant.merchantName}` : ''}.
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Track invoice health, payment progress, and reconciliation outcomes from one place.
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button type="primary" ghost onClick={() => navigate('/invoices')}>
            Invoices
          </Button>
          <Button type="primary" onClick={() => navigate('/settings/payment-config')}>
            Payment config
          </Button>
          <Button onClick={logout}>Log out</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} className="summary-grid">
        <Col xs={12} lg={6}>
          <Card>
            <Statistic title="Total invoices" value={summary?.totalInvoices ?? 0} />
          </Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card>
            <Statistic title="Paid" value={summary?.paidCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card>
            <Statistic title="Unpaid" value={summary?.unpaidCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card>
            <Statistic title="Exceptions" value={summary?.exceptionCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} className="content-grid">
        <Col xs={24} xl={12}>
          <Card
            title="Received amount"
            extra={summaryQuery.isFetching ? <Spin size="small" /> : null}
            className="glass-card"
          >
            <Typography.Title level={2} style={{ marginTop: 0 }}>
              {formatAmount(summary?.totalReceivedAmount ?? 0)} USDC
            </Typography.Title>
            <Typography.Paragraph type="secondary">
              Verified on-chain receipts across the current merchant account.
            </Typography.Paragraph>
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card
            title="Invoice status distribution"
            extra={statusQuery.isFetching ? <Spin size="small" /> : null}
            className="glass-card"
          >
            {statusQuery.isLoading ? (
              <div className="center-empty">
                <Spin />
              </div>
            ) : distribution.length > 0 ? (
              <div className="status-list">
                {distribution.map((item) => (
                  <StatusCard key={item.status} item={item} />
                ))}
              </div>
            ) : (
              <Empty description="No status distribution yet" />
            )}
          </Card>
        </Col>
      </Row>

      {(summaryQuery.error || statusQuery.error) && !(summaryQuery.error instanceof ApiError && summaryQuery.error.status === 401) ? (
        <Card className="error-card">
          <Typography.Text type="danger">
            {(summaryQuery.error as Error | undefined)?.message ?? (statusQuery.error as Error | undefined)?.message ?? 'Failed to load dashboard'}
          </Typography.Text>
        </Card>
      ) : null}
    </div>
  );
}
