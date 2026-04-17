import { request } from './http';

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface CreateInvoiceRequest {
  customerName: string;
  amount: number;
  description?: string;
  expireAt: string;
}

export interface InvoiceListQuery {
  status?: string;
  page?: number;
  size?: number;
}

export interface InvoiceListItem {
  id: number;
  publicId: string;
  invoiceNo: string;
  customerName: string;
  amount: number | string;
  currency: string;
  status: string;
  expireAt: string;
  createdAt: string;
}

export interface InvoicePaymentInfo {
  recipientAddress: string;
  referenceKey: string;
  mintAddress: string;
  expectedAmount: number | string;
  paymentLink: string;
  label: string;
  message: string;
  expireAt: string;
}

export interface InvoicePaymentStatus {
  invoiceId: number;
  publicId: string;
  invoiceNo: string;
  status: string;
  exceptionTags: string[];
  paidAt?: string | null;
  lastProcessedAt?: string | null;
  latestTxHash?: string | null;
  latestVerificationResult?: string | null;
  latestPaymentStatus?: string | null;
}

export interface InvoicePaymentProof {
  invoiceId: number;
  publicId: string;
  invoiceNo: string;
  txHash: string;
  referenceKey?: string | null;
  payerAddress?: string | null;
  recipientAddress?: string | null;
  mintAddress: string;
  amount: number | string;
  paidAt?: string | null;
  verificationResult?: string | null;
  finalStatus: string;
  exceptionTags: string[];
  reconciliationStatus?: string | null;
  resultMessage?: string | null;
  createdAt: string;
}

export interface InvoiceDetail {
  id: number;
  publicId: string;
  invoiceNo: string;
  customerName: string;
  amount: number | string;
  currency: string;
  chain: string;
  description?: string;
  status: string;
  expireAt: string;
  paidAt?: string | null;
  createdAt: string;
  paymentInfo?: InvoicePaymentInfo | null;
}

export interface ManualSubmitPaymentRequest {
  invoiceId: number;
  txHash: string;
}

export interface ManualSubmitPaymentResult {
  invoiceId: number;
  paymentTransactionId: number;
  txHash: string;
  referenceKey?: string | null;
  verificationResult: string;
  paymentTransactionStatus: string;
  reconciledCount: number;
  paymentStatus: InvoicePaymentStatus;
  message: string;
}

export function createInvoice(requestBody: CreateInvoiceRequest): Promise<InvoiceDetail> {
  return request<InvoiceDetail>('/invoices', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}

export function listInvoices(query: InvoiceListQuery): Promise<PageResult<InvoiceListItem>> {
  return request<PageResult<InvoiceListItem>>('/invoices/list', {
    method: 'POST',
    body: JSON.stringify({
      status: query.status,
      page: query.page ?? 1,
      size: query.size ?? 10
    })
  });
}

export function getInvoiceDetail(id: number): Promise<InvoiceDetail> {
  return request<InvoiceDetail>('/invoices/detail', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function activateInvoice(id: number): Promise<InvoiceDetail> {
  return request<InvoiceDetail>('/invoices/activate', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function cancelInvoice(id: number): Promise<InvoiceDetail> {
  return request<InvoiceDetail>('/invoices/cancel', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function getInvoicePaymentInfo(id: number): Promise<InvoicePaymentInfo> {
  return request<InvoicePaymentInfo>('/invoices/payment-info', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function getInvoicePaymentStatus(id: number): Promise<InvoicePaymentStatus> {
  return request<InvoicePaymentStatus>('/invoices/payment-status', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function getInvoicePaymentProof(id: number): Promise<InvoicePaymentProof> {
  return request<InvoicePaymentProof>('/invoices/payment-proof', {
    method: 'POST',
    body: JSON.stringify({ id })
  });
}

export function manualSubmitInvoicePayment(requestBody: ManualSubmitPaymentRequest): Promise<ManualSubmitPaymentResult> {
  return request<ManualSubmitPaymentResult>('/invoices/payment/manual-submit', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}
