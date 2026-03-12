package com.stableflow.blockchain.dto;

public record GetTransactionOptionsDto(
    /** Response encoding format / 返回结果编码格式 */
    String encoding,
    /** Commitment level used by the RPC node / RPC 节点使用的确认级别 */
    String commitment,
    /** Max supported transaction version / 支持的最大交易版本 */
    Integer maxSupportedTransactionVersion
) {
}
