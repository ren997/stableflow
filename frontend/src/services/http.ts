import { clearSession, getAccessToken } from './session';

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  status: number;
  code?: number;

  constructor(message: string, status: number, code?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

function isEnvelope<T>(payload: unknown): payload is ApiEnvelope<T> {
  return Boolean(
    payload &&
      typeof payload === 'object' &&
      'code' in payload &&
      'message' in payload &&
      'data' in payload
  );
}

async function readBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function getErrorMessage(payload: unknown, fallback: string): string {
  if (isEnvelope(payload) && typeof payload.message === 'string' && payload.message.length > 0) {
    return payload.message;
  }

  if (payload && typeof payload === 'object' && 'message' in payload) {
    const message = (payload as { message?: unknown }).message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }

  if (typeof payload === 'string' && payload.length > 0) {
    return payload;
  }

  return fallback;
}

async function requestInternal<T>(
  path: string,
  init: RequestInit = {},
  options: { includeAuth: boolean; clearOnUnauthorized: boolean }
): Promise<T> {
  const headers = new Headers(init.headers);
  const token = options.includeAuth ? getAccessToken() : null;

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  if (init.body != null && !headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`/api${path}`, {
    ...init,
    headers
  });

  const payload = await readBody(response);

  if (!response.ok) {
    if (response.status === 401 && options.clearOnUnauthorized) {
      clearSession();
    }

    throw new ApiError(getErrorMessage(payload, response.statusText), response.status, isEnvelope(payload) ? payload.code : undefined);
  }

  if (isEnvelope<T>(payload)) {
    return payload.data;
  }

  return payload as T;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  return requestInternal<T>(path, init, {
    includeAuth: true,
    clearOnUnauthorized: true
  });
}

export async function publicRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  return requestInternal<T>(path, init, {
    includeAuth: false,
    clearOnUnauthorized: false
  });
}
