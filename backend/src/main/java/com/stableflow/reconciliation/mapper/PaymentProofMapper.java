package com.stableflow.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.reconciliation.entity.PaymentProof;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentProofMapper extends BaseMapper<PaymentProof> {

    int insertJsonb(
        @Param("invoiceId") Long invoiceId,
        @Param("txHash") String txHash,
        @Param("proofType") String proofType,
        @Param("proofPayloadJson") String proofPayloadJson
    );
}
