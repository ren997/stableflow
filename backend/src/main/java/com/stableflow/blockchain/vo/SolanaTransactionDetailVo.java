package com.stableflow.blockchain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record SolanaTransactionDetailVo(
    String signature,
    Long slot,
    OffsetDateTime blockTime,
    JsonNode rawTransaction
) {
}
