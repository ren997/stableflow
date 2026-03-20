package com.stableflow.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    /** Query daily verified payment trend for one merchant / 查询商家按日维度的已验证收款趋势 */
    @Select("""
        SELECT
            date_trunc('day', pt.block_time) AS bucketStartAt,
            COALESCE(SUM(pt.amount), 0) AS totalReceivedAmount,
            COUNT(*) AS transactionCount
        FROM payment_transaction pt
        WHERE pt.invoice_id IN (SELECT id FROM invoice WHERE merchant_id = #{merchantId})
          AND pt.verification_result IN ('PAID', 'PARTIALLY_PAID', 'OVERPAID')
        GROUP BY date_trunc('day', pt.block_time)
        ORDER BY date_trunc('day', pt.block_time)
        """)
    List<Map<String, Object>> listDailyVerifiedTrendByMerchantId(@Param("merchantId") Long merchantId);

    /** Query weekly verified payment trend for one merchant / 查询商家按周维度的已验证收款趋势 */
    @Select("""
        SELECT
            date_trunc('week', pt.block_time) AS bucketStartAt,
            COALESCE(SUM(pt.amount), 0) AS totalReceivedAmount,
            COUNT(*) AS transactionCount
        FROM payment_transaction pt
        WHERE pt.invoice_id IN (SELECT id FROM invoice WHERE merchant_id = #{merchantId})
          AND pt.verification_result IN ('PAID', 'PARTIALLY_PAID', 'OVERPAID')
        GROUP BY date_trunc('week', pt.block_time)
        ORDER BY date_trunc('week', pt.block_time)
        """)
    List<Map<String, Object>> listWeeklyVerifiedTrendByMerchantId(@Param("merchantId") Long merchantId);

    /** Query monthly verified payment trend for one merchant / 查询商家按月维度的已验证收款趋势 */
    @Select("""
        SELECT
            date_trunc('month', pt.block_time) AS bucketStartAt,
            COALESCE(SUM(pt.amount), 0) AS totalReceivedAmount,
            COUNT(*) AS transactionCount
        FROM payment_transaction pt
        WHERE pt.invoice_id IN (SELECT id FROM invoice WHERE merchant_id = #{merchantId})
          AND pt.verification_result IN ('PAID', 'PARTIALLY_PAID', 'OVERPAID')
        GROUP BY date_trunc('month', pt.block_time)
        ORDER BY date_trunc('month', pt.block_time)
        """)
    List<Map<String, Object>> listMonthlyVerifiedTrendByMerchantId(@Param("merchantId") Long merchantId);
}
