import { request } from './http';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  merchantId: number;
  email: string;
  merchantName: string;
}

export function login(requestBody: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}
