package com.stableflow.blockchain.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SolanaTransactionDetailVo {

    /** Transaction signature / 交易签名 */
    private String signature;

    /** Slot number / 槽位号 */
    private Long slot;

    /** Block time in UTC / 区块时间（UTC） */
    private OffsetDateTime blockTime;

    /** Whether transaction execution succeeded / 交易是否执行成功 */
    private Boolean success;

    /** Raw execution error if failed / 交易失败原始错误 */
    private String error;

    /** Network fee in lamports / 网络手续费（lamports） */
    private Long fee;

    /** Payer or transfer authority wallet / 付款钱包或转账授权钱包 */
    private String payerAddress;

    /** Transfer destination address / 转账目标地址 */
    private String recipientAddress;

    /** Source token account / 来源 token 账户 */
    private String sourceAddress;

    /** Token mint address / 代币 Mint 地址 */
    private String mintAddress;

    /** Parsed transfer amount / 解析出的转账金额 */
    private BigDecimal amount;

    /** Parsed transfer type / 解析出的转账类型 */
    private String transferType;

    /** Reference candidates extracted from the transaction / 交易中提取出的 reference 候选 */
    private List<String> referenceKeys;

    /** First reference candidate for matching / 首个 reference 候选 */
    private String primaryReferenceKey;

    /** Serialized raw RPC transaction payload / 序列化后的原始 RPC 交易载荷 */
    private String rawPayload;
}
