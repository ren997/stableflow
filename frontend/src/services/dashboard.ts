import { request } from './http';
import type { PageResult } from './invoice';

export interface DashboardSummary {
  totalInvoices: number;
  paidCount: number;
  unpaidCount: number;
  exceptionCount: number;
  totalReceivedAmount: string | number;
}

export interface DashboardStatusCountItem {
  status: string;
  count: number;
}

export interface DashboardExceptionInvoiceQuery {
  exceptionTag?: string;
  page?: number;
  size?: number;
}

export interface DashboardExceptionInvoiceItem {
  id: number;
  publicId: string;
  invoiceNo: string;
  customerName: string;
  amount: number | string;
  currency: string;
  status: string;
  exceptionTags: string[];
  expireAt?: string | null;
  createdAt: string;
}

function normalizeStatusCountItems(payload: unknown): DashboardStatusCountItem[] {
  const unwrap = (value: unknown): DashboardStatusCountItem[] => {
    if (Array.isArray(value)) {
      return value
        .map((item) => {
          if (!item || typeof item !== 'object') {
            return null;
          }

          const status = (item as { status?: unknown }).status;
          const count = (item as { count?: unknown }).count;

          if (typeof status !== 'string') {
            return null;
          }

          return {
            status,
            count: typeof count === 'number' ? count : Number(count ?? 0)
          };
        })
        .filter((item): item is DashboardStatusCountItem => item !== null);
    }

    if (!value || typeof value !== 'object') {
      return [];
    }

    const candidate =
      (value as { items?: unknown }).items ??
      (value as { statusCounts?: unknown }).statusCounts ??
      (value as { records?: unknown }).records;

    return unwrap(candidate);
  };

  return unwrap(payload);
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return request<DashboardSummary>('/dashboard/summary', {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export async function getDashboardStatusDistribution(): Promise<DashboardStatusCountItem[]> {
  const payload = await request<unknown>('/dashboard/invoices/status', {
    method: 'POST'
  });
  return normalizeStatusCountItems(payload);
}

export function getDashboardExceptionInvoices(
  query: DashboardExceptionInvoiceQuery
): Promise<PageResult<DashboardExceptionInvoiceItem>> {
  return request<PageResult<DashboardExceptionInvoiceItem>>('/dashboard/invoices/exceptions', {
    method: 'POST',
    body: JSON.stringify({
      exceptionTag: query.exceptionTag,
      page: query.page ?? 1,
      size: query.size ?? 10
    })
  });
}
