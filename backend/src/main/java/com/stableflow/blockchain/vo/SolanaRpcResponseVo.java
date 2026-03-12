package com.stableflow.blockchain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.blockchain.vo.SolanaRpcErrorVo;

public record SolanaRpcResponseVo(
    JsonNode result,
    SolanaRpcErrorVo error,
    int id
) {
    public boolean hasError() {
        return error != null;
    }
}
