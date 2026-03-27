import { request } from './http';

export interface MerchantPaymentConfig {
  id: number;
  merchantId: number;
  walletAddress: string;
  mintAddress: string;
  chain: string;
  activeFlag: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MerchantPaymentConfigRequest {
  walletAddress: string;
  mintAddress: string;
  chain: string;
}

export function getMerchantPaymentConfig(): Promise<MerchantPaymentConfig> {
  return request<MerchantPaymentConfig>('/merchant/payment-config/get', {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export function saveMerchantPaymentConfig(
  requestBody: MerchantPaymentConfigRequest
): Promise<MerchantPaymentConfig> {
  return request<MerchantPaymentConfig>('/merchant/payment-config', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}
