package com.stableflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.dashboard.vo.DashboardExceptionInvoiceVo;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.api.PageResult;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    @Override
    public DashboardInvoiceStatusDistributionVo getInvoiceStatusDistribution() {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        List<DashboardInvoiceStatusDistributionVo.StatusCountItem> items = Arrays.stream(InvoiceStatusEnum.values())
            .map(status -> new DashboardInvoiceStatusDistributionVo.StatusCountItem(
                status,
                invoiceService.count(merchantQuery(merchantId).eq(Invoice::getStatus, status))
            ))
            .toList();
        return new DashboardInvoiceStatusDistributionVo(items);
    }

    @Override
    public PageResult<DashboardExceptionInvoiceVo> getExceptionInvoices(ExceptionTagEnum exceptionTag, int page, int size) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        LambdaQueryWrapper<Invoice> query = merchantQuery(merchantId)
            .and(wrapper -> wrapper
                .in(Invoice::getStatus, EXCEPTION_STATUSES)
                .or()
                .isNotNull(Invoice::getExceptionTags)
            )
            .orderByDesc(Invoice::getCreatedAt);

        if (exceptionTag != null) {
            query.apply("exception_tags @> CAST({0} AS jsonb)", "[\"" + exceptionTag.getCode() + "\"]");
        }

        IPage<Invoice> pageResult = invoiceService.page(new Page<>(page, size), query);
        return new PageResult<>(
            pageResult.getRecords().stream().map(this::toExceptionInvoiceVo).toList(),
            pageResult.getTotal(),
            pageResult.getCurrent(),
            pageResult.getSize()
        );
    }

    private LambdaQueryWrapper<Invoice> merchantQuery(Long merchantId) {
        return new LambdaQueryWrapper<Invoice>().eq(Invoice::getMerchantId, merchantId);
    }

    private DashboardExceptionInvoiceVo toExceptionInvoiceVo(Invoice invoice) {
        return new DashboardExceptionInvoiceVo(
            invoice.getId(),
            invoice.getPublicId(),
            invoice.getInvoiceNo(),
            invoice.getCustomerName(),
            invoice.getAmount(),
            invoice.getCurrency(),
            invoice.getStatus(),
            normalizeExceptionTags(invoice.getExceptionTags()),
            invoice.getExpireAt(),
            invoice.getCreatedAt()
        );
    }

    private List<ExceptionTagEnum> normalizeExceptionTags(List<String> exceptionTags) {
        if (exceptionTags == null || exceptionTags.isEmpty()) {
            return List.of();
        }
        return exceptionTags.stream()
            .filter(tag -> tag != null && !tag.isBlank())
            .map(String::trim)
            .distinct()
            .map(ExceptionTagEnum::fromCode)
            .filter(Objects::nonNull)
            .toList();
    }
}
