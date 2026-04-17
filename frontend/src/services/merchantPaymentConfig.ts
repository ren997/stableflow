import { request } from './http';

export type MerchantWalletOwnershipStatus =
  | 'UNVERIFIED'
  | 'CHALLENGE_ISSUED'
  | 'SIGNATURE_SUBMITTED'
  | 'VERIFIED'
  | 'FAILED';

export interface MerchantPaymentConfig {
  id: number;
  merchantId: number;
  walletAddress: string;
  mintAddress: string;
  chain: string;
  activeFlag: boolean;
  ownershipVerificationStatus: MerchantWalletOwnershipStatus;
  ownershipChallengeExpiresAt?: string | null;
  ownershipSignatureSubmittedAt?: string | null;
  ownershipVerifiedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MerchantPaymentConfigRequest {
  walletAddress: string;
}

export interface MerchantWalletOwnershipChallenge {
  configId: number;
  walletAddress: string;
  chain: string;
  ownershipVerificationStatus: MerchantWalletOwnershipStatus;
  challengeCode: string;
  challengeMessage: string;
  challengeExpiresAt: string;
}

export interface MerchantWalletOwnershipVerifyRequest {
  challengeCode: string;
  signature: string;
}

export interface MerchantWalletOwnershipVerifyResult {
  configId: number;
  walletAddress: string;
  ownershipVerificationStatus: MerchantWalletOwnershipStatus;
  verifierReady: boolean;
  verificationMessage: string;
  challengeExpiresAt?: string | null;
  signatureSubmittedAt?: string | null;
  verifiedAt?: string | null;
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

export function createMerchantWalletOwnershipChallenge(): Promise<MerchantWalletOwnershipChallenge> {
  return request<MerchantWalletOwnershipChallenge>('/merchant/payment-config/ownership/challenge', {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export function verifyMerchantWalletOwnership(
  requestBody: MerchantWalletOwnershipVerifyRequest
): Promise<MerchantWalletOwnershipVerifyResult> {
  return request<MerchantWalletOwnershipVerifyResult>('/merchant/payment-config/ownership/verify', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}
