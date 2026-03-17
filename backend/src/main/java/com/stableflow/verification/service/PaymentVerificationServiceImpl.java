package com.stableflow.verification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import com.stableflow.verification.vo.PaymentVerificationResultVo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

    private static final Set<String> EFFECTIVE_VERIFICATION_RESULTS = Set.of(
        PaymentVerificationResultEnum.PAID.getCode(),
        PaymentVerificationResultEnum.PARTIALLY_PAID.getCode(),
        PaymentVerificationResultEnum.OVERPAID.getCode(),
        PaymentVerificationResultEnum.LATE_PAYMENT.getCode()
    );

    private final PaymentTransactionService paymentTransactionService;
    private final InvoicePaymentRequestMapper invoicePaymentRequestMapper;
    private final InvoiceService invoiceService;

    public PaymentVerificationServiceImpl(
        PaymentTransactionService paymentTransactionService,
        InvoicePaymentRequestMapper invoicePaymentRequestMapper,
        InvoiceService invoiceService
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.invoicePaymentRequestMapper = invoicePaymentRequestMapper;
        this.invoiceService = invoiceService;
    }

    @Transactional
    @Override
    public PaymentVerificationResultVo verifyTransaction(Long paymentTransactionId) {
        PaymentTransaction paymentTransaction = paymentTransactionService.getById(paymentTransactionId);
        if (paymentTransaction == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Payment transaction not found");
        }
        return verifyTransaction(paymentTransaction);
    }

    @Transactional
    @Override
    public PaymentVerificationResultVo verifyTransaction(PaymentTransaction paymentTransaction) {
        if (paymentTransaction == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Payment transaction is required");
        }

        VerificationDecision decision = decideVerification(paymentTransaction);
        applyDecision(paymentTransaction, decision);
        return new PaymentVerificationResultVo(
            paymentTransaction.getId(),
            decision.invoiceId(),
            paymentTransaction.getTxHash(),
            paymentTransaction.getReferenceKey(),
            decision.verificationResult().getCode(),
            decision.paymentStatus().getCode(),
            decision.message()
        );
    }

    private VerificationDecision decideVerification(PaymentTransaction paymentTransaction) {
        if (paymentTransaction.getReferenceKey() == null || paymentTransaction.getReferenceKey().isBlank()) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.MISSING_REFERENCE,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Transaction does not contain a usable reference key."
            );
        }

        InvoicePaymentRequest paymentRequest = invoicePaymentRequestMapper.selectOne(
            new LambdaQueryWrapper<InvoicePaymentRequest>()
                .eq(InvoicePaymentRequest::getReferenceKey, paymentTransaction.getReferenceKey())
        );
        if (paymentRequest == null) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.INVALID_REFERENCE,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Reference key does not match any invoice payment request."
            );
        }

        Invoice invoice = invoiceService.getById(paymentRequest.getInvoiceId());
        if (invoice == null) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.INVALID_REFERENCE,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Reference key points to a missing invoice."
            );
        }

        if (!matchesMintAddress(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.WRONG_CURRENCY,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Transaction mint address does not match the invoice payment request."
            );
        }

        if (isLatePayment(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.LATE_PAYMENT,
                PaymentTransactionStatusEnum.EXPIRED,
                "Transaction arrived after the invoice payment request expired."
            );
        }

        if (hasEarlierEffectivePayment(invoice.getId(), paymentTransaction)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.DUPLICATE_PAYMENT,
                PaymentTransactionStatusEnum.DUPLICATE,
                "An earlier effective payment already exists for this invoice."
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
                "Transaction amount matches the invoice expected amount."
            );
        }
        if (compareResult < 0) {
            return new VerificationDecision(
                invoiceId,
                PaymentVerificationResultEnum.PARTIALLY_PAID,
                PaymentTransactionStatusEnum.PARTIALLY_PAID,
                "Transaction amount is lower than the invoice expected amount."
            );
        }
        return new VerificationDecision(
            invoiceId,
            PaymentVerificationResultEnum.OVERPAID,
            PaymentTransactionStatusEnum.OVERPAID,
            "Transaction amount is greater than the invoice expected amount."
        );
    }

    private void applyDecision(PaymentTransaction paymentTransaction, VerificationDecision decision) {
        paymentTransaction.setInvoiceId(decision.invoiceId());
        paymentTransaction.setVerificationResult(decision.verificationResult().getCode());
        paymentTransaction.setPaymentStatus(decision.paymentStatus().getCode());

        if (paymentTransaction.getId() == null) {
            return;
        }

        PaymentTransaction update = new PaymentTransaction();
        update.setId(paymentTransaction.getId());
        update.setInvoiceId(decision.invoiceId());
        update.setVerificationResult(decision.verificationResult().getCode());
        update.setPaymentStatus(decision.paymentStatus().getCode());
        paymentTransactionService.updateById(update);
    }

    private boolean matchesMintAddress(PaymentTransaction paymentTransaction, InvoicePaymentRequest paymentRequest) {
        String expectedMintAddress = paymentRequest.getMintAddress();
        String actualMintAddress = paymentTransaction.getMintAddress();
        if (expectedMintAddress == null || expectedMintAddress.isBlank()) {
            return true;
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Payment transaction amount is missing");
        }
        return paymentTransaction.getAmount();
    }

    private record VerificationDecision(
        Long invoiceId,
        PaymentVerificationResultEnum verificationResult,
        PaymentTransactionStatusEnum paymentStatus,
        String message
    ) {
    }
}
