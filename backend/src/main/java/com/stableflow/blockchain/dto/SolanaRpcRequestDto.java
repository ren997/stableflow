package com.stableflow.blockchain.dto;

import java.util.List;

public record SolanaRpcRequestDto(
    // Solana RPC uses JSON-RPC 2.0 in the HTTP request body.
    String jsonrpc,
    int id,
    // RPC method name, for example getSignaturesForAddress or getTransaction.
    String method,
    // Parameters required by the target RPC method.
    List<Object> params
) {

    public static SolanaRpcRequestDto of(int id, String method, List<Object> params) {
        return new SolanaRpcRequestDto("2.0", id, method, params);
    }
}
