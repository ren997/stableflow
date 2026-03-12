package com.stableflow.blockchain.client;

import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.util.List;

public interface SolanaClient {

    List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit);

    SolanaTransactionDetailVo getTransaction(String signature);
}
