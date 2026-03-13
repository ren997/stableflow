package com.stableflow.blockchain.client;

import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.util.List;

public interface SolanaClient {

    /** Query the latest transaction signatures for a recipient address / 查询收款地址最近一批交易签名 */
    List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit);

    /** Query transaction signatures before a specific signature for incremental pagination / 基于指定签名前翻页查询交易签名 */
    List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit, String beforeSignature);

    /** Query and parse a transaction into business-ready fields / 查询并解析单笔交易为业务可用字段 */
    SolanaTransactionDetailVo getTransaction(String signature);
}
