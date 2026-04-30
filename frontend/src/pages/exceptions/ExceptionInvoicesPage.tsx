import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Button,
  Card,
  Empty,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getDashboardExceptionInvoices, type DashboardExceptionInvoiceItem } from '../../services/dashboard';
import { ApiError } from '../../services/http';
import { clearSession } from '../../services/session';

const exceptionTagOptions = [
  { value: '', label: 'All exception tags' },
  { value: 'LATE_PAYMENT', label: 'Late payment' },
  { value: 'WRONG_CURRENCY', label: 'Wrong currency' },
  { value: 'DUPLICATE_PAYMENT', label: 'Duplicate payment' },
  { value: 'UNMATCHED_PAYMENT', label: 'Unmatched payment' },
  { value: 'MISSING_REFERENCE', label: 'Missing reference' },
  { value: 'INVALID_REFERENCE', label: 'Invalid reference' },
  { value: 'PAYMENT_DELAYED', label: 'Payment delayed' }
];

const statusMeta: Record<string, { label: string; color: string }> = {
  PARTIALLY_PAID: { label: 'Partial', color: 'gold' },
  OVERPAID: { label: 'Overpaid', color: 'volcano' },
  EXPIRED: { label: 'Expired', color: 'warning' },
  FAILED_RECONCILIATION: { label: 'Failed', color: 'red' },
  PENDING: { label: 'Pending', color: 'processing' },
  PAID: { label: 'Paid', color: 'success' }
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

function InvoiceStatusTag({ status }: { status: string }) {
  const meta = statusMeta[status] ?? { label: status, color: 'default' };
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function ExceptionInvoicesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [exceptionTag, setExceptionTag] = useState('');

  const exceptionInvoicesQuery = useQuery({
    queryKey: ['dashboard-exception-invoices', exceptionTag, page, pageSize],
    queryFn: () =>
      getDashboardExceptionInvoices({
        exceptionTag: exceptionTag || undefined,
        page,
        size: pageSize
      }),
    retry: false
  });

  useEffect(() => {
    if (exceptionInvoicesQuery.error instanceof ApiError && exceptionInvoicesQuery.error.status === 401) {
      clearSession();
      message.error('Session expired. Please sign in again.');
      navigate('/login', { replace: true });
    }
  }, [exceptionInvoicesQuery.error, navigate]);

  const columns = useMemo<ColumnsType<DashboardExceptionInvoiceItem>>(
    () => [
      {
        title: 'Invoice',
        key: 'invoice',
        render: (_value, record) => (
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
        render: (_value, record) => `${formatAmount(record.amount)} ${record.currency}`
      },
      {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
        render: (status: string) => <InvoiceStatusTag status={status} />
      },
      {
        title: 'Exception tags',
        key: 'exceptionTags',
        render: (_value, record) => (
          record.exceptionTags.length > 0 ? (
            <Space wrap>
              {record.exceptionTags.map((tag) => (
                <Tag key={tag} color="warning">
                  {tag}
                </Tag>
              ))}
            </Space>
          ) : '-'
        )
      },
      {
        title: 'Expires',
        dataIndex: 'expireAt',
        key: 'expireAt',
        render: (value: string | null) => formatDateTime(value)
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
        render: (_value, record) => (
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
          <Typography.Text className="eyebrow">Exceptions</Typography.Text>
          <Typography.Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>
            Review invoices that need attention
          </Typography.Title>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Track partial payments, overpayments, expirations, and reconciliation failures in one focused queue.
          </Typography.Paragraph>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/dashboard')}>
            Back to dashboard
          </Button>
          <Button type="primary" onClick={() => navigate('/invoices')}>
            All invoices
          </Button>
        </Space>
      </div>

      <Card
        className="glass-card invoice-table-card"
        title="Exception invoice queue"
        extra={(
          <Space wrap className="invoice-filter-bar">
            <Select
              value={exceptionTag}
              options={exceptionTagOptions}
              style={{ minWidth: 220 }}
              onChange={(value) => {
                setExceptionTag(value);
                setPage(1);
              }}
            />
            <Button onClick={() => exceptionInvoicesQuery.refetch()} loading={exceptionInvoicesQuery.isFetching}>
              Refresh
            </Button>
          </Space>
        )}
      >
        <Table<DashboardExceptionInvoiceItem>
          rowKey="id"
          columns={columns}
          dataSource={exceptionInvoicesQuery.data?.records ?? []}
          loading={exceptionInvoicesQuery.isLoading}
          pagination={{
            current: exceptionInvoicesQuery.data?.page ?? page,
            pageSize: exceptionInvoicesQuery.data?.size ?? pageSize,
            total: exceptionInvoicesQuery.data?.total ?? 0,
            onChange: (nextPage, nextPageSize) => {
              setPage(nextPage);
              setPageSize(nextPageSize);
            }
          }}
          locale={{
            emptyText: <Empty description="No exception invoices found" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          }}
          onRow={(record) => ({
            onClick: () => navigate(`/invoices/${record.id}`)
          })}
        />
      </Card>

      {exceptionInvoicesQuery.error instanceof ApiError && exceptionInvoicesQuery.error.status !== 401 ? (
        <Card className="error-card">
          <Typography.Text type="danger">{exceptionInvoicesQuery.error.message}</Typography.Text>
        </Card>
      ) : null}
    </div>
  );
}
