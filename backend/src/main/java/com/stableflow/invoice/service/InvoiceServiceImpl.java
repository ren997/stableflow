package com.stableflow.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.service.ReconciliationRecordService;
import com.stableflow.system.config.PaymentProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    private static final String DEFAULT_CHAIN = "SOLANA";
    private static final String DEFAULT_CURRENCY = "USDC";

    private final InvoiceMapper invoiceMapper;
    private final InvoicePaymentRequestMapper invoicePaymentRequestMapper;
    private final MerchantPaymentConfigService merchantPaymentConfigService;
    private final CurrentMerchantProvider currentMerchantProvider;
    private final PaymentProperties paymentProperties;
    private final PaymentTransactionService paymentTransactionService;
    private final ReconciliationRecordService reconciliationRecordService;

    @Transactional
    @Override
    public InvoiceDetailVo createInvoice(CreateInvoiceRequestDto request) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        MerchantPaymentConfig paymentConfig = merchantPaymentConfigService.getRequiredConfig(merchantId);

        Invoice invoice = new Invoice();
        invoice.setPublicId(generatePublicId());
        invoice.setMerchantId(merchantId);
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setCustomerName(request.customerName());
        invoice.setAmount(request.amount());
        invoice.setCurrency(normalizeOrDefault(request.currency(), DEFAULT_CURRENCY));
        invoice.setChain(normalizeOrDefault(request.chain(), DEFAULT_CHAIN));
        invoice.setDescription(request.description());
        invoice.setStatus(InvoiceStatusEnum.PENDING);
        invoice.setExpireAt(request.expireAt());
        invoiceMapper.insert(invoice);

        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setInvoiceId(invoice.getId());
        paymentRequest.setRecipientAddress(paymentConfig.getWalletAddress());
        paymentRequest.setReferenceKey(generateReferenceKey());
        paymentRequest.setMintAddress(paymentConfig.getMintAddress());
        paymentRequest.setExpectedAmount(request.amount());
        paymentRequest.setLabel("StableFlow Invoice");
        paymentRequest.setMessage(invoice.getInvoiceNo());
        paymentRequest.setExpireAt(request.expireAt());
        paymentRequest.setPaymentLink(buildPaymentLink(paymentRequest));
        invoicePaymentRequestMapper.insert(paymentRequest);

        return toDetailResponse(invoice, paymentRequest);
    }

    @Override
    public List<InvoiceListItemVo> listInvoices(String status) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<Invoice>()
            .eq(Invoice::getMerchantId, merchantId)
            .orderByDesc(Invoice::getCreatedAt);
        if (status != null && !status.isBlank()) {
            InvoiceStatusEnum statusEnum = InvoiceStatusEnum.fromCode(status.toUpperCase(Locale.ROOT));
            if (statusEnum == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Unsupported invoice status: " + status);
            }
            wrapper.eq(Invoice::getStatus, statusEnum);
        }
        return invoiceMapper.selectList(wrapper).stream().map(this::toListItemResponse).toList();
    }

    @Override
    public InvoiceDetailVo getInvoiceDetail(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        InvoicePaymentRequest paymentRequest = getPaymentRequest(invoiceId);
        return toDetailResponse(invoice, paymentRequest);
    }

    @Override
    public PaymentInfoVo getPaymentInfo(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        return toPaymentInfoResponse(getPaymentRequest(invoice.getId()));
    }

    @Override
    public PaymentStatusVo getPaymentStatus(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        PaymentTransaction latestTransaction = paymentTransactionService.getLatestTransactionByInvoiceId(invoice.getId());
        ReconciliationRecord latestReconciliationRecord = reconciliationRecordService.getLatestRecordByInvoiceId(invoice.getId());

        return new PaymentStatusVo(
            invoice.getId(),
            invoice.getPublicId(),
            invoice.getInvoiceNo(),
            invoice.getStatus(),
            splitExceptionTags(invoice.getExceptionTags()),
            invoice.getPaidAt(),
            resolveLastProcessedAt(latestReconciliationRecord, latestTransaction),
            latestTransaction == null ? null : latestTransaction.getTxHash(),
            latestTransaction == null ? null : latestTransaction.getVerificationResult(),
            latestTransaction == null ? null : latestTransaction.getPaymentStatus()
        );
    }

    private Invoice getOwnedInvoice(Long invoiceId) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null || !merchantId.equals(invoice.getMerchantId())) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND);
        }
        return invoice;
    }

    private InvoicePaymentRequest getPaymentRequest(Long invoiceId) {
        InvoicePaymentRequest paymentRequest = invoicePaymentRequestMapper.selectOne(
            new LambdaQueryWrapper<InvoicePaymentRequest>().eq(InvoicePaymentRequest::getInvoiceId, invoiceId)
        );
        if (paymentRequest == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Payment request not found");
        }
        return paymentRequest;
    }

    private InvoiceDetailVo toDetailResponse(Invoice invoice, InvoicePaymentRequest paymentRequest) {
        return new InvoiceDetailVo(
            invoice.getId(),
            invoice.getPublicId(),
            invoice.getInvoiceNo(),
            invoice.getCustomerName(),
            invoice.getAmount(),
            invoice.getCurrency(),
            invoice.getChain(),
            invoice.getDescription(),
            invoice.getStatus(),
            invoice.getExpireAt(),
            invoice.getPaidAt(),
            invoice.getCreatedAt(),
            toPaymentInfoResponse(paymentRequest)
        );
    }

    private InvoiceListItemVo toListItemResponse(Invoice invoice) {
        return new InvoiceListItemVo(
            invoice.getId(),
            invoice.getPublicId(),
            invoice.getInvoiceNo(),
            invoice.getCustomerName(),
            invoice.getAmount(),
            invoice.getCurrency(),
            invoice.getStatus(),
            invoice.getExpireAt(),
            invoice.getCreatedAt()
        );
    }

    private PaymentInfoVo toPaymentInfoResponse(InvoicePaymentRequest paymentRequest) {
        return new PaymentInfoVo(
            paymentRequest.getRecipientAddress(),
            paymentRequest.getReferenceKey(),
            paymentRequest.getMintAddress(),
            paymentRequest.getExpectedAmount(),
            paymentRequest.getPaymentLink(),
            paymentRequest.getLabel(),
            paymentRequest.getMessage(),
            paymentRequest.getExpireAt()
        );
    }

    private String buildPaymentLink(InvoicePaymentRequest paymentRequest) {
        return "solana:" + paymentRequest.getRecipientAddress()
            + "?amount=" + paymentRequest.getExpectedAmount().toPlainString()
            + "&spl-token=" + encode(paymentRequest.getMintAddress())
            + "&reference=" + encode(paymentRequest.getReferenceKey())
            + "&label=" + encode(paymentRequest.getLabel())
            + "&message=" + encode(paymentRequest.getMessage());
    }

    private String generatePublicId() {
        return "pub_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateReferenceKey() {
        return "ref_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateInvoiceNo() {
        return "INV-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .format(OffsetDateTime.now(ZoneOffset.UTC))
            + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.toUpperCase(Locale.ROOT);
    }

    private List<String> splitExceptionTags(String exceptionTags) {
        if (exceptionTags == null || exceptionTags.isBlank()) {
            return List.of();
        }
        return List.of(exceptionTags.split(",")).stream()
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
    }

    private OffsetDateTime resolveLastProcessedAt(
        ReconciliationRecord latestReconciliationRecord,
        PaymentTransaction latestTransaction
    ) {
        if (latestReconciliationRecord != null && latestReconciliationRecord.getProcessedAt() != null) {
            return latestReconciliationRecord.getProcessedAt();
        }
        return latestTransaction == null ? null : latestTransaction.getBlockTime();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
