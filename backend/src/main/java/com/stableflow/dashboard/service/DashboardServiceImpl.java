package com.stableflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final List<InvoiceStatusEnum> EXCEPTION_STATUSES = List.of(
        InvoiceStatusEnum.PARTIALLY_PAID,
        InvoiceStatusEnum.OVERPAID,
        InvoiceStatusEnum.EXPIRED,
        InvoiceStatusEnum.FAILED_RECONCILIATION
    );

    private static final List<InvoiceStatusEnum> UNPAID_STATUSES = List.of(
        InvoiceStatusEnum.DRAFT,
        InvoiceStatusEnum.PENDING
    );

    private final InvoiceService invoiceService;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CurrentMerchantProvider currentMerchantProvider;

    @Override
    public DashboardSummaryVo getSummary() {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();

        // 1. 总账单数
        long totalInvoices = invoiceService.count(merchantQuery(merchantId));

        // 2. 已支付数
        long paidCount = invoiceService.count(
            merchantQuery(merchantId).eq(Invoice::getStatus, InvoiceStatusEnum.PAID)
        );

        // 3. 待支付数
        long unpaidCount = invoiceService.count(
            merchantQuery(merchantId).in(Invoice::getStatus, UNPAID_STATUSES)
        );

        // 4. 异常数
        long exceptionCount = invoiceService.count(
            merchantQuery(merchantId).in(Invoice::getStatus, EXCEPTION_STATUSES)
        );

        // 5. 链上已验证收款总额
        BigDecimal totalReceivedAmount = paymentTransactionMapper.sumVerifiedAmountByMerchantId(merchantId);
        if (totalReceivedAmount == null) {
            totalReceivedAmount = BigDecimal.ZERO;
        }

        return new DashboardSummaryVo(
            totalInvoices,
            paidCount,
            unpaidCount,
            exceptionCount,
            totalReceivedAmount
        );
    }

    private LambdaQueryWrapper<Invoice> merchantQuery(Long merchantId) {
        return new LambdaQueryWrapper<Invoice>().eq(Invoice::getMerchantId, merchantId);
    }
}
