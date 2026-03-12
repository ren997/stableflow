package com.stableflow.blockchain.dto;

import java.util.List;

public record SolanaRpcRequestDto(
    String jsonrpc,
    int id,
    String method,
    List<Object> params
) {

    public static SolanaRpcRequestDto of(int id, String method, List<Object> params) {
        return new SolanaRpcRequestDto("2.0", id, method, params);
    }
}
