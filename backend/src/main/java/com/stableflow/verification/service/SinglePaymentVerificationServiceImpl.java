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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional single-payment verification service / 负责单笔候选交易事务验证的服务 */
@Service
@RequiredArgsConstructor
public class SinglePaymentVerificationServiceImpl implements SinglePaymentVerificationService {

    private static final Set<PaymentVerificationResultEnum> EFFECTIVE_VERIFICATION_RESULTS = Set.of(
        PaymentVerificationResultEnum.PAID,
        PaymentVerificationResultEnum.PARTIALLY_PAID,
        PaymentVerificationResultEnum.OVERPAID,
        PaymentVerificationResultEnum.LATE_PAYMENT
    );

    private final PaymentTransactionService paymentTransactionService;
    private final InvoicePaymentRequestMapper invoicePaymentRequestMapper;
    private final InvoiceService invoiceService;

    @Transactional
    @Override
    public PaymentVerificationResultVo verifyTransaction(Long paymentTransactionId) {
        // 先按主键取出候选交易，供后续按统一规则执行验证。
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

        // 先得出验证结论，再把结论回写到 payment_transaction。
        VerificationDecision decision = decideVerification(paymentTransaction);
        applyDecision(paymentTransaction, decision);
        return new PaymentVerificationResultVo(
            paymentTransaction.getId(),
            decision.invoiceId(),
            paymentTransaction.getTxHash(),
            paymentTransaction.getReferenceKey(),
            decision.verificationResult(),
            decision.paymentStatus(),
            decision.message()
        );
    }

    private VerificationDecision decideVerification(PaymentTransaction paymentTransaction) {
        // 第一步先看链上交易里有没有可用 reference，没有就无法继续认账。
        if (paymentTransaction.getReferenceKey() == null || paymentTransaction.getReferenceKey().isBlank()) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.MISSING_REFERENCE,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Transaction does not contain a usable reference key."
            );
        }

        // 第二步通过 reference_key 命中支付请求，这是交易和账单之间的第一层关联。
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

        // 第三步再从支付请求拿到目标 invoice，形成 reference -> payment request -> invoice 的完整链路。
        Invoice invoice = invoiceService.getById(paymentRequest.getInvoiceId());
        if (invoice == null) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.INVALID_REFERENCE,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Reference key points to a missing invoice."
            );
        }
        if (invoice.getStatus() == com.stableflow.invoice.enums.InvoiceStatusEnum.DRAFT) {
            return new VerificationDecision(
                null,
                PaymentVerificationResultEnum.PENDING,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Draft invoice is not active yet and cannot enter payment verification."
            );
        }

        // 先校验币种，避免错误资产直接进入金额判断。
        if (!matchesMintAddress(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.WRONG_CURRENCY,
                PaymentTransactionStatusEnum.UNMATCHED,
                "Transaction mint address does not match the invoice payment request."
            );
        }

        // 再校验支付时间窗口，过期到账先单独标记，不和正常支付结果混在一起。
        if (isLatePayment(paymentTransaction, paymentRequest)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.LATE_PAYMENT,
                PaymentTransactionStatusEnum.EXPIRED,
                "Transaction arrived after the invoice payment request expired."
            );
        }

        // 如果这个账单已经有更早的有效支付，就把当前交易判成重复支付。
        if (hasEarlierEffectivePayment(invoice.getId(), paymentTransaction)) {
            return new VerificationDecision(
                invoice.getId(),
                PaymentVerificationResultEnum.DUPLICATE_PAYMENT,
                PaymentTransactionStatusEnum.DUPLICATE,
                "An earlier effective payment already exists for this invoice."
            );
        }

        // 前面的基础校验都通过后，再进入金额比较，决定是足额、少付还是多付。
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
        // 先更新内存对象，保证调用方能立即拿到最新验证结果。
        paymentTransaction.setInvoiceId(decision.invoiceId());
        paymentTransaction.setVerificationResult(decision.verificationResult());
        paymentTransaction.setPaymentStatus(decision.paymentStatus());

        if (paymentTransaction.getId() == null) {
            return;
        }

        // 再把最关键的认账结果落回数据库，供后续 T402/T403 批量处理与核销复用。
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
        // 只看已经判定为有效支付的历史交易，避免把未验证或异常交易当成重复支付依据。
        List<PaymentTransaction> effectiveTransactions = paymentTransactionService.list(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getInvoiceId, invoiceId)
                .ne(paymentTransaction.getId() != null, PaymentTransaction::getId, paymentTransaction.getId())
                .in(PaymentTransaction::getVerificationResult, EFFECTIVE_VERIFICATION_RESULTS)
        );

        return effectiveTransactions.stream().anyMatch(existingTransaction -> isEarlierTransaction(existingTransaction, paymentTransaction));
    }

    private boolean isEarlierTransaction(PaymentTransaction existingTransaction, PaymentTransaction currentTransaction) {
        // 优先按 blockTime 判断先后；如果链上时间缺失，再退化到主键顺序兜底。
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
        /** Matched invoice id after reference-based association / 基于 reference 关联后的账单 ID */
        Long invoiceId,
        /** Final verification result code for the transaction / 交易最终验证结果 */
        PaymentVerificationResultEnum verificationResult,
        /** Derived payment status used by downstream processing / 供后续流程使用的派生支付状态 */
        PaymentTransactionStatusEnum paymentStatus,
        /** Human-readable verification message / 可读验证说明 */
        String message
    ) {
    }
}
