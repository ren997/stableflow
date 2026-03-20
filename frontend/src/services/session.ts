export interface MerchantSession {
  merchantId: number;
  merchantName: string;
  email: string;
}

export interface AuthenticatedMerchantSession extends MerchantSession {
  accessToken: string;
}

const ACCESS_TOKEN_KEY = 'stableflow.accessToken';
const MERCHANT_SESSION_KEY = 'stableflow.merchantSession';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getMerchantSession(): MerchantSession | null {
  const raw = localStorage.getItem(MERCHANT_SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as MerchantSession;
  } catch {
    return null;
  }
}

export function setMerchantSession(session: MerchantSession): void {
  localStorage.setItem(MERCHANT_SESSION_KEY, JSON.stringify(session));
}

export function setAuthenticatedMerchantSession(session: AuthenticatedMerchantSession): void {
  setAccessToken(session.accessToken);
  setMerchantSession({
    merchantId: session.merchantId,
    merchantName: session.merchantName,
    email: session.email
  });
}

export function clearSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(MERCHANT_SESSION_KEY);
}
