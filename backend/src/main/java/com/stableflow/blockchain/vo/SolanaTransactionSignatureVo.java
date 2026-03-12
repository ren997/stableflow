package com.stableflow.blockchain.vo;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SolanaTransactionSignatureVo {

    /** Transaction signature / 交易签名 */
    private String signature;

    /** Slot number / 槽位号 */
    private Long slot;

    /** Block time in UTC / 区块时间（UTC） */
    private OffsetDateTime blockTime;

    /** Raw error payload if present / 原始错误信息 */
    private String error;
}
