import { request } from './http';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  merchantName: string;
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

export function register(requestBody: RegisterRequest): Promise<LoginResponse> {
  return request<LoginResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  });
}
