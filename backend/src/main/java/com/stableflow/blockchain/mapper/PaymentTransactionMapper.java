package com.stableflow.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {

    /** Sum verified payment amounts for a merchant's invoices / 汇总商家所有已验证交易的收款金额 */
    @Select("""
        SELECT COALESCE(SUM(pt.amount), 0)
        FROM payment_transaction pt
        WHERE pt.invoice_id IN (SELECT id FROM invoice WHERE merchant_id = #{merchantId})
          AND pt.verification_result IN ('PAID', 'PARTIALLY_PAID', 'OVERPAID')
        """)
    BigDecimal sumVerifiedAmountByMerchantId(@Param("merchantId") Long merchantId);
}
