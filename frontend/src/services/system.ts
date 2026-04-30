import { request } from './http';

export type SolanaNetwork = 'DEVNET' | 'MAINNET';

export interface SystemRuntimeConfig {
  solanaNetwork: SolanaNetwork;
  defaultMintAddress: string;
  explorerTxBaseUrl: string;
}

export function getSystemRuntimeConfig(): Promise<SystemRuntimeConfig> {
  return request<SystemRuntimeConfig>('/system/runtime-config', {
    method: 'POST'
  });
}
