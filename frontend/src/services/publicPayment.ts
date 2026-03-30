import { publicRequest } from './http';
import type { InvoicePaymentInfo } from './invoice';

export interface PublicPaymentPage {
  publicId: string;
  invoiceNo: string;
  customerName: string;
  amount: number | string;
  currency: string;
  chain: string;
  description?: string | null;
  status: string;
  expireAt: string;
  paidAt?: string | null;
  paymentInfo: InvoicePaymentInfo;
}

export function getPublicPaymentPage(publicId: string): Promise<PublicPaymentPage> {
  return publicRequest<PublicPaymentPage>(`/pay/${publicId}`, {
    method: 'GET'
  });
}
