package com.stableflow.blockchain.vo;

import java.time.OffsetDateTime;

public record SolanaTransactionSignatureVo(
    String signature,
    Long slot,
    OffsetDateTime blockTime,
    String err
) {
}
