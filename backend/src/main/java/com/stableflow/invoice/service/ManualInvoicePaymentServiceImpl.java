package com.stableflow.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.client.SolanaClient;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.invoice.dto.ManualSubmitPaymentRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.ManualSubmitPaymentVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.reconciliation.service.SingleReconciliationService;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sol4k.PublicKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Invoice-scoped manual payment submission orchestration / 面向账单的手动支付提交编排服务 */
@Service
@RequiredArgsConstructor
public class ManualInvoicePaymentServiceImpl implements ManualInvoicePaymentService {

    private static final Set<PaymentVerificationResultEnum> EFFECTIVE_VERIFICATION_RESULTS = Set.of(
        PaymentVerificationResultEnum.PAID,
        PaymentVerificationResultEnum.PARTIALLY_PAID,
        PaymentVerificationResultEnum.OVERPAID,
        PaymentVerificationResultEnum.LATE_PAYMENT
    );

    private final InvoiceMapper invoiceMapper;
    private final InvoicePaymentRequestMapper invoicePaymentRequestMapper;
    private final CurrentMerchantProvider currentMerchantProvider;
    private final SolanaClient solanaClient;
    private final PaymentTransactionService paymentTransactionService;
    private final InvoiceService invoiceService;
    private final SingleReconciliationService singleReconciliationService;
    private final SolanaProperties solanaProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public ManualSubmitPaymentVo submitPayment(ManualSubmitPaymentRequestDto request) {
        Invoice invoice = getOwnedInvoice(request.invoiceId());
        ensureInvoiceAcceptsManualSubmission(invoice);
        InvoicePaymentRequest paymentRequest = getInvoicePaymentRequest(invoice.getId());

        PaymentTransaction paymentTransaction = paymentTransactionService.getByTxHash(request.txHash().trim());
        if (paymentTransaction == null) {
            SolanaTransactionDetailVo transactionDetail = loadSupportedTransaction(request.txHash().trim());
            validateReferenceCompatibility(transactionDetail.getPrimaryReferenceKey(), paymentRequest);
            validateRecipientMatch(transactionDetail.getRecipientAddress(), paymentRequest);
            paymentTransaction = persistFetchedTransaction(transactionDetail);
        } else {
            validateReferenceCompatibility(paymentTransaction.getReferenceKey(), paymentRequest);
            validateRecipientMatch(paymentTransaction.getRecipientAddress(), paymentRequest);
        }

        ensureTransactionNotBoundToAnotherInvoice(paymentTransaction, invoice.getId());

        VerificationDecision decision = decideManualVerification(paymentTransaction, paymentRequest, invoice);
        applyDecision(paymentTransaction, decision);

        int reconciledCount = singleReconciliationService.reconcileTransaction(paymentTransaction) ? 1 : 0;
        PaymentStatusVo paymentStatus = invoiceService.getPaymentStatus(invoice.getId());

        return new ManualSubmitPaymentVo(
            invoice.getId(),
            paymentTransaction.getId(),
            paymentTransaction.getTxHash(),
            paymentTransaction.getReferenceKey(),
            paymentTransaction.getVerificationResult(),
            paymentTransaction.getPaymentStatus(),
            reconciledCount,
            paymentStatus,
            decision.message()
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

    private void ensureInvoiceAcceptsManualSubmission(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatusEnum.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Draft invoice must be activated before manual payment submission");
        }
        if (invoice.getStatus() == InvoiceStatusEnum.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cancelled invoice no longer accepts manual payment submission");
        }
    }

    private InvoicePaymentRequest getInvoicePaymentRequest(Long invoiceId) {
        InvoicePaymentRequest paymentRequest = invoicePaymentRequestMapper.selectOne(
            new LambdaQueryWrapper<InvoicePaymentRequest>().eq(InvoicePaymentRequest::getInvoiceId, invoiceId)
        );
        if (paymentRequest == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Payment request not found for invoice");
        }
        return paymentRequest;
    }

    private SolanaTransactionDetailVo loadSupportedTransaction(String txHash) {
        SolanaTransactionDetailVo transactionDetail = solanaClient.getTransaction(txHash);
        if (transactionDetail == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Transaction hash was not found on chain");
        }
        if (Boolean.FALSE.equals(transactionDetail.getSuccess())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Transaction failed on chain and cannot be reconciled");
        }
        if (!hasText(transactionDetail.getRecipientAddress()) || transactionDetail.getAmount() == null) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Transaction is not a supported transfer for manual invoice reconciliation"
            );
        }
        return transactionDetail;
    }

    private PaymentTransaction persistFetchedTransaction(SolanaTransactionDetailVo transactionDetail) {
        PaymentTransaction newTransaction = toPaymentTransaction(transactionDetail);
        paymentTransactionService.saveIfAbsent(newTransaction);
        PaymentTransaction persistedTransaction = paymentTransactionService.getByTxHash(newTransaction.getTxHash());
        if (persistedTransaction == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to persist manual payment transaction");
        }
        return persistedTransaction;
    }

    private PaymentTransaction toPaymentTransaction(SolanaTransactionDetailVo transactionDetail) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTxHash(transactionDetail.getSignature());
        paymentTransaction.setReferenceKey(transactionDetail.getPrimaryReferenceKey());
        paymentTransaction.setPayerAddress(transactionDetail.getPayerAddress());
        paymentTransaction.setRecipientAddress(transactionDetail.getRecipientAddress());
        paymentTransaction.setAmount(transactionDetail.getAmount());
        paymentTransaction.setCurrency(resolveCurrency(transactionDetail.getMintAddress()));
        paymentTransaction.setMintAddress(transactionDetail.getMintAddress());
        paymentTransaction.setBlockTime(transactionDetail.getBlockTime());
        paymentTransaction.setVerificationResult(PaymentVerificationResultEnum.PENDING);
        paymentTransaction.setPaymentStatus(PaymentTransactionStatusEnum.DETECTED);
        paymentTransaction.setRawPayload(parseRawPayload(transactionDetail.getRawPayload()));
        return paymentTransaction;
    }

    private void validateReferenceCompatibility(String transactionReferenceKey, InvoicePaymentRequest paymentRequest) {
        if (!hasText(transactionReferenceKey)) {
            return;
        }
        if (!transactionReferenceKey.equals(paymentRequest.getReferenceKey())) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Transaction carries a different reference and cannot be manually matched to this invoice"
            );
        }
    }

    private void validateRecipientMatch(String transactionRecipientAddress, InvoicePaymentRequest paymentRequest) {
        if (!hasText(transactionRecipientAddress)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Transaction recipient is missing");
        }

        LinkedHashSet<String> allowedRecipients = new LinkedHashSet<>();
        addIfPresent(allowedRecipients, paymentRequest.getRecipientAddress());
        addIfPresent(
            allowedRecipients,
            deriveAssociatedTokenAddress(paymentRequest.getRecipientAddress(), paymentRequest.getMintAddress())
        );

        if (!allowedRecipients.contains(transactionRecipientAddress)) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Transaction recipient does not match the invoice payment destination"
            );
        }
    }

    private void ensureTransactionNotBoundToAnotherInvoice(PaymentTransaction paymentTransaction, Long invoiceId) {
        if (paymentTransaction.getInvoiceId() != null && !invoiceId.equals(paymentTransaction.getInvoiceId())) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Transaction has already been associated with another invoice"
            );
        }
    }

    private VerificationDecision decideManualVerification(
        PaymentTransaction paymentTransaction,
        InvoicePaymentRequest paymentRequest,
        Invoice invoice
    ) {
        if (!matchesMintAddress(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.WRONG_CURRENCY,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Transaction was found, but the mint address does not match the invoice payment request."
            );
        }

        if (isLatePayment(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.LATE_PAYMENT,
                PaymentTransactionStatusEnum.EXPIRED,
                buildMessage(paymentTransaction, "Transaction was manually matched, but it arrived after invoice expiry.")
            );
        }

        if (hasEarlierEffectivePayment(invoice.getId(), paymentTransaction)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.DUPLICATE_PAYMENT,
                PaymentTransactionStatusEnum.DUPLICATE,
                buildMessage(paymentTransaction, "Transaction was manually matched, but an earlier effective payment already exists.")
            );
        }

        return decideAmountBasedResult(paymentTransaction, paymentRequest, invoice.getId());
    }

    private VerificationDecision decideAmountBasedResult(
        PaymentTransaction paymentTransaction,
        InvoicePaymentRequest paymentRequest,
        Long invoiceId
    ) {
        BigDecimal amount = requireAmount(paymentTransaction);
        int compareResult = amount.compareTo(paymentRequest.getExpectedAmount());

        if (compareResult == 0) {
            return new VerificationDecision(
                invoiceId,
                PaymentVerificationResultEnum.PAID,
                PaymentTransactionStatusEnum.PAID,
                buildMessage(paymentTransaction, "Transaction amount matches the invoice expected amount.")
            );
        }
        if (compareResult < 0) {
            return new VerificationDecision(
                invoiceId,
                PaymentVerificationResultEnum.PARTIALLY_PAID,
                PaymentTransactionStatusEnum.PARTIALLY_PAID,
                buildMessage(paymentTransaction, "Transaction amount is lower than the invoice expected amount.")
            );
        }
        return new VerificationDecision(
            invoiceId,
            PaymentVerificationResultEnum.OVERPAID,
            PaymentTransactionStatusEnum.OVERPAID,
            buildMessage(paymentTransaction, "Transaction amount is greater than the invoice expected amount.")
        );
    }

    private String buildMessage(PaymentTransaction paymentTransaction, String baseMessage) {
        if (hasText(paymentTransaction.getReferenceKey())) {
            return baseMessage;
        }
        return baseMessage + " The invoice was matched through manual tx hash submission because the transaction carries no on-chain reference.";
    }

    private void applyDecision(PaymentTransaction paymentTransaction, VerificationDecision decision) {
        paymentTransaction.setInvoiceId(decision.invoiceId());
        paymentTransaction.setVerificationResult(decision.verificationResult());
        paymentTransaction.setPaymentStatus(decision.paymentStatus());

        if (paymentTransaction.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Manual payment transaction must be persisted before verification update");
        }

        PaymentTransaction update = new PaymentTransaction();
        update.setId(paymentTransaction.getId());
        update.setInvoiceId(decision.invoiceId());
        update.setVerificationResult(decision.verificationResult());
        update.setPaymentStatus(decision.paymentStatus());
        paymentTransactionService.updateById(update);
    }

    private boolean matchesMintAddress(PaymentTransaction paymentTransaction, InvoicePaymentRequest paymentRequest) {
        String expectedMintAddress = paymentRequest.getMintAddress();
        String actualMintAddress = paymentTransaction.getMintAddress();
        if (!hasText(expectedMintAddress)) {
            return !hasText(actualMintAddress);
        }
        return expectedMintAddress.equals(actualMintAddress);
    }

    private boolean isLatePayment(PaymentTransaction paymentTransaction, InvoicePaymentRequest paymentRequest) {
        OffsetDateTime expireAt = paymentRequest.getExpireAt();
        OffsetDateTime blockTime = paymentTransaction.getBlockTime();
        return expireAt != null && blockTime != null && blockTime.isAfter(expireAt);
    }

    private boolean hasEarlierEffectivePayment(Long invoiceId, PaymentTransaction paymentTransaction) {
        List<PaymentTransaction> effectiveTransactions = paymentTransactionService.list(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getInvoiceId, invoiceId)
                .ne(paymentTransaction.getId() != null, PaymentTransaction::getId, paymentTransaction.getId())
                .in(PaymentTransaction::getVerificationResult, EFFECTIVE_VERIFICATION_RESULTS)
        );

        return effectiveTransactions.stream().anyMatch(existingTransaction -> isEarlierTransaction(existingTransaction, paymentTransaction));
    }

    private boolean isEarlierTransaction(PaymentTransaction existingTransaction, PaymentTransaction currentTransaction) {
        OffsetDateTime existingBlockTime = existingTransaction.getBlockTime();
        OffsetDateTime currentBlockTime = currentTransaction.getBlockTime();

        if (existingBlockTime != null && currentBlockTime != null) {
            int compareResult = existingBlockTime.compareTo(currentBlockTime);
            if (compareResult < 0) {
                return true;
            }
            if (compareResult > 0) {
                return false;
            }
        } else if (existingBlockTime != null) {
            return true;
        } else if (currentBlockTime != null) {
            return false;
        }

        Long existingId = existingTransaction.getId();
        Long currentId = currentTransaction.getId();
        return existingId != null && currentId != null && existingId < currentId;
    }

    private BigDecimal requireAmount(PaymentTransaction paymentTransaction) {
        if (paymentTransaction.getAmount() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Transaction amount is missing");
        }
        return paymentTransaction.getAmount();
    }

    private String deriveAssociatedTokenAddress(String walletAddress, String mintAddress) {
        if (!hasText(walletAddress) || !hasText(mintAddress)) {
            return null;
        }
        try {
            return PublicKey.findProgramDerivedAddress(new PublicKey(walletAddress), new PublicKey(mintAddress))
                .getPublicKey()
                .toBase58();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveCurrency(String mintAddress) {
        if (!hasText(mintAddress)) {
            return "UNKNOWN";
        }
        return mintAddress.equals(solanaProperties.resolvedUsdcMintAddress()) ? "USDC" : "UNKNOWN";
    }

    private JsonNode parseRawPayload(String rawPayload) {
        if (!hasText(rawPayload)) {
            return null;
        }
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Failed to parse Solana raw payload: " + ex.getOriginalMessage()
            );
        }
    }

    private void addIfPresent(LinkedHashSet<String> values, String candidate) {
        if (hasText(candidate)) {
            values.add(candidate);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record VerificationDecision(
        Long invoiceId,
        PaymentVerificationResultEnum verificationResult,
        PaymentTransactionStatusEnum paymentStatus,
        String message
    ) {
    }
}
