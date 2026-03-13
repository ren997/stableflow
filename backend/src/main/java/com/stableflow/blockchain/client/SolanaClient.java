package com.stableflow.blockchain.client;

import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.util.List;

/**
 * Solana blockchain access contract used by the StableFlow hackathon MVP / StableFlow 黑客松 MVP 使用的 Solana 链上访问契约
 *
 * <p>This interface exists to keep RPC access, transaction discovery, and transaction parsing behind a small boundary,
 * so the payment scan and verification flow can focus on business rules instead of raw Solana RPC details.
 * / 这个接口把 RPC 调用、交易发现和交易解析收敛在一个小边界内，让支付扫描和验证流程聚焦业务规则，而不是直接处理底层 Solana RPC 细节。
 */
public interface SolanaClient {

    /**
     * Query the latest transaction signatures for a recipient address in the MVP scan flow / 查询 MVP 扫描流程中收款地址最近一批交易签名
     *
     * @param address recipient wallet address to scan / 需要扫描的收款钱包地址
     * @param limit max number of signatures to return in one request / 单次请求最多返回的签名数量
     * @return latest signature summaries for the address / 该地址最近一批交易签名摘要
     */
    List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit);

    /**
     * Query older signatures before a specific signature so the MVP can scan incrementally instead of full rescans / 基于指定签名前翻页查询更早的交易签名，让 MVP 能做增量扫描而不是全量回扫
     *
     * @param address recipient wallet address to scan / 需要扫描的收款钱包地址
     * @param limit max number of signatures to return in one page / 单页最多返回的签名数量
     * @param beforeSignature pagination cursor that points to an already seen signature / 指向已扫描签名的分页游标
     * @return older signature summaries before the cursor / 游标之前更早的一批交易签名摘要
     */
    List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit, String beforeSignature);

    /**
     * Query and parse a transaction into business-ready payment fields for the hackathon reconciliation loop / 查询并解析单笔交易为黑客松核销闭环可直接使用的支付字段
     *
     * @param signature blockchain transaction signature / 链上交易签名
     * @return parsed transaction result ready for scan, verification, and reconciliation / 可直接用于扫描、验证和核销的交易解析结果
     */
    SolanaTransactionDetailVo getTransaction(String signature);
}
