package com.stableflow.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.dto.ActivateInvoiceRequestDto;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.dto.UpdateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.invoice.vo.PublicPaymentPageVo;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.system.api.PageResult;
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
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    private static final String DEFAULT_CHAIN = "SOLANA";
    private static final String DEFAULT_CURRENCY = "USDC";
    private static final char[] BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] BASE58_INDEXES = new int[128];
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        java.util.Arrays.fill(BASE58_INDEXES, -1);
        for (int i = 0; i < BASE58_ALPHABET.length; i++) {
            BASE58_INDEXES[BASE58_ALPHABET[i]] = i;
        }
    }

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
        invoice.setCurrency(DEFAULT_CURRENCY);
        invoice.setChain(resolveInvoiceChain(paymentConfig));
        invoice.setDescription(request.description());
        invoice.setStatus(InvoiceStatusEnum.DRAFT);
        invoice.setExpireAt(request.expireAt());
        invoiceMapper.insert(invoice);

        InvoicePaymentRequest paymentRequest = buildPaymentRequest(invoice, paymentConfig);
        invoicePaymentRequestMapper.insert(paymentRequest);

        return toDetailResponse(invoice, paymentRequest);
    }

    @Transactional
    @Override
    public InvoiceDetailVo activateInvoice(ActivateInvoiceRequestDto request) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        Invoice invoice = getOwnedInvoice(request.id());
        if (invoice.getStatus() != InvoiceStatusEnum.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only DRAFT invoices can be activated");
        }

        MerchantPaymentConfig paymentConfig = merchantPaymentConfigService.getRequiredConfig(merchantId);
        InvoicePaymentRequest paymentRequest = findPaymentRequest(invoice.getId());
        if (paymentRequest == null) {
            paymentRequest = buildPaymentRequest(invoice, paymentConfig);
            invoicePaymentRequestMapper.insert(paymentRequest);
        } else {
            paymentRequest.setRecipientAddress(paymentConfig.getWalletAddress());
            paymentRequest.setReferenceKey(generateReferenceKey());
            paymentRequest.setMintAddress(paymentConfig.getMintAddress());
            paymentRequest.setExpectedAmount(invoice.getAmount());
            paymentRequest.setLabel("StableFlow Invoice");
            paymentRequest.setMessage(invoice.getInvoiceNo());
            paymentRequest.setExpireAt(invoice.getExpireAt());
            paymentRequest.setPaymentLink(buildPaymentLink(paymentRequest));
            invoicePaymentRequestMapper.updateById(paymentRequest);
        }

        applyStatusTransition(invoice, InvoiceStatusEnum.PENDING);
        invoiceMapper.updateById(invoice);
        return toDetailResponse(invoice, paymentRequest);
    }

    @Transactional
    @Override
    public InvoiceDetailVo cancelInvoice(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        if (invoice.getStatus() != InvoiceStatusEnum.DRAFT && invoice.getStatus() != InvoiceStatusEnum.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only DRAFT or PENDING invoices can be cancelled");
        }

        applyStatusTransition(invoice, InvoiceStatusEnum.CANCELLED);
        invoiceMapper.updateById(invoice);
        return toDetailResponse(invoice, findPaymentRequest(invoiceId));
    }

    @Transactional
    @Override
    public InvoiceDetailVo updateInvoice(UpdateInvoiceRequestDto request) {
        Invoice invoice = getOwnedInvoice(request.id());
        validateEditableInvoice(invoice);
        InvoicePaymentRequest paymentRequest = findPaymentRequest(invoice.getId());

        invoice.setCustomerName(request.customerName());
        invoice.setAmount(request.amount());
        invoice.setDescription(request.description());
        invoice.setExpireAt(request.expireAt());

        invoiceMapper.updateById(invoice);

        if (paymentRequest != null) {
            paymentRequest.setExpectedAmount(invoice.getAmount());
            paymentRequest.setPaymentLink(buildPaymentLink(paymentRequest));
            paymentRequest.setExpireAt(invoice.getExpireAt());
            paymentRequest.setMessage(invoice.getInvoiceNo());
            invoicePaymentRequestMapper.updateById(paymentRequest);
        }

        return toDetailResponse(invoice, paymentRequest);
    }

    @Override
    public PageResult<InvoiceListItemVo> listInvoices(String status, ExceptionTagEnum exceptionTag, int page, int size) {
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
        if (exceptionTag != null) {
            wrapper.apply("exception_tags @> CAST({0} AS jsonb)", "[\"" + exceptionTag.getCode() + "\"]");
        }
        IPage<Invoice> pageResult = invoiceMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(
            pageResult.getRecords().stream().map(this::toListItemResponse).toList(),
            pageResult.getTotal(),
            pageResult.getCurrent(),
            pageResult.getSize()
        );
    }

    @Override
    public InvoiceDetailVo getInvoiceDetail(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        InvoicePaymentRequest paymentRequest = findPaymentRequest(invoiceId);
        return toDetailResponse(invoice, paymentRequest);
    }

    @Override
    public PaymentInfoVo getPaymentInfo(Long invoiceId) {
        Invoice invoice = getOwnedInvoice(invoiceId);
        ensureInvoiceActivated(invoice);
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
            normalizeExceptionTags(invoice.getExceptionTags()),
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
        InvoicePaymentRequest paymentRequest = findPaymentRequest(invoiceId);
        if (paymentRequest == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Payment request not found");
        }
        return paymentRequest;
    }

    private InvoicePaymentRequest findPaymentRequest(Long invoiceId) {
        return invoicePaymentRequestMapper.selectOne(
            new LambdaQueryWrapper<InvoicePaymentRequest>().eq(InvoicePaymentRequest::getInvoiceId, invoiceId)
        );
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
            hidesPaymentInfo(invoice.getStatus()) || paymentRequest == null ? null : toPaymentInfoResponse(paymentRequest)
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
        if (paymentRequest == null) {
            return null;
        }
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

    private InvoicePaymentRequest buildPaymentRequest(Invoice invoice, MerchantPaymentConfig paymentConfig) {
        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setInvoiceId(invoice.getId());
        paymentRequest.setRecipientAddress(paymentConfig.getWalletAddress());
        paymentRequest.setReferenceKey(generateReferenceKey());
        paymentRequest.setMintAddress(paymentConfig.getMintAddress());
        paymentRequest.setExpectedAmount(invoice.getAmount());
        paymentRequest.setLabel("StableFlow Invoice");
        paymentRequest.setMessage(invoice.getInvoiceNo());
        paymentRequest.setExpireAt(invoice.getExpireAt());
        paymentRequest.setPaymentLink(buildPaymentLink(paymentRequest));
        return paymentRequest;
    }

    private String generatePublicId() {
        return "pub_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateReferenceKey() {
        byte[] referenceBytes = new byte[32];
        SECURE_RANDOM.nextBytes(referenceBytes);
        return encodeBase58(referenceBytes);
    }

    private String generateInvoiceNo() {
        return "INV-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .format(OffsetDateTime.now(ZoneOffset.UTC))
            + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.toUpperCase(Locale.ROOT);
    }

    private String resolveInvoiceChain(MerchantPaymentConfig paymentConfig) {
        return normalizeOrDefault(paymentConfig.getChain(), DEFAULT_CHAIN);
    }

    private String encodeBase58(byte[] input) {
        if (input.length == 0) {
            return "";
        }

        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            zeros++;
        }

        byte[] encoded = new byte[input.length * 2];
        int outputStart = encoded.length;
        byte[] copy = java.util.Arrays.copyOf(input, input.length);

        for (int inputStart = zeros; inputStart < copy.length; ) {
            int remainder = divmod58(copy, inputStart);
            encoded[--outputStart] = (byte) BASE58_ALPHABET[remainder];
            if (copy[inputStart] == 0) {
                inputStart++;
            }
        }

        while (outputStart < encoded.length && encoded[outputStart] == BASE58_ALPHABET[0]) {
            outputStart++;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = (byte) BASE58_ALPHABET[0];
        }
        return new String(encoded, outputStart, encoded.length - outputStart, StandardCharsets.US_ASCII);
    }

    private int divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit = number[i] & 0xFF;
            int temp = remainder * 256 + digit;
            number[i] = (byte) (temp / 58);
            remainder = temp % 58;
        }
        return remainder;
    }

    private void applyStatusTransition(Invoice invoice, InvoiceStatusEnum targetStatus) {
        try {
            InvoiceStatusEnum.ensureCanTransition(invoice.getStatus(), targetStatus);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, ex.getMessage());
        }
        invoice.setStatus(targetStatus);
    }

    private void validateEditableInvoice(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatusEnum.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only DRAFT invoices can be edited");
        }
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
            .filter(java.util.Objects::nonNull)
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

    @Override
    public PublicPaymentPageVo getPublicPaymentPage(String publicId) {
        // 1. 按 publicId 查询账单，不校验商家归属
        Invoice invoice = invoiceMapper.selectOne(
            new LambdaQueryWrapper<Invoice>().eq(Invoice::getPublicId, publicId)
        );
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND);
        }
        if (hidesPaymentInfo(invoice.getStatus())) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Public payment page is not available for inactive invoices");
        }

        // 2. 获取支付请求快照
        InvoicePaymentRequest paymentRequest = getPaymentRequest(invoice.getId());

        return new PublicPaymentPageVo(
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
            toPaymentInfoResponse(paymentRequest)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void ensureInvoiceActivated(Invoice invoice) {
        if (hidesPaymentInfo(invoice.getStatus())) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Inactive invoices do not expose payment info before activation or after cancellation"
            );
        }
    }

    private boolean hidesPaymentInfo(InvoiceStatusEnum status) {
        return status == InvoiceStatusEnum.DRAFT || status == InvoiceStatusEnum.CANCELLED;
    }

    @Transactional
    @Override
    public int expirePendingInvoices() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 只挑选已经到期、仍处于待支付且尚未落最终支付时间的账单，避免误伤已完成核销的记录。
        List<Invoice> expiredCandidates = invoiceMapper.selectList(
            new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getStatus, InvoiceStatusEnum.PENDING)
                .isNull(Invoice::getPaidAt)
                .isNotNull(Invoice::getExpireAt)
                .le(Invoice::getExpireAt, now)
                .orderByAsc(Invoice::getExpireAt)
        );

        int expiredCount = 0;
        for (Invoice invoice : expiredCandidates) {
            try {
                InvoiceStatusEnum.ensureCanTransition(invoice.getStatus(), InvoiceStatusEnum.EXPIRED);
            } catch (IllegalStateException ex) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, ex.getMessage());
            }
            // 逐条按当前状态做条件更新，避免任务与并发支付处理直接互相覆盖。
            int updatedRows = invoiceMapper.update(
                null,
                new UpdateWrapper<Invoice>()
                    .eq("id", invoice.getId())
                    .eq("status", InvoiceStatusEnum.PENDING.getCode())
                    .isNull("paid_at")
                    .isNotNull("expire_at")
                    .le("expire_at", now)
                    .set("status", InvoiceStatusEnum.EXPIRED.getCode())
            );
            expiredCount += updatedRows;
        }

        log.info(
            "Invoice expiration sweep finished, now={}, candidateCount={}, expiredCount={}",
            now,
            expiredCandidates.size(),
            expiredCount
        );
        return expiredCount;
    }
}
