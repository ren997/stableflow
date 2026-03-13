package com.stableflow.blockchain.dto;

public record GetSignaturesForAddressOptionsDto(
    /** Max number of signatures to return / 最大返回签名数量 */
    Integer limit,
    /** Return results before this signature / 返回该签名之前的结果 */
    String before,
    /** Commitment level used by the RPC node / RPC 节点使用的确认级别 */
    String commitment
) {
}
