package com.stableflow.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.reconciliation.entity.PaymentProof;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.enums.PaymentProofTypeEnum;
import com.stableflow.reconciliation.mapper.PaymentProofMapper;
import com.stableflow.reconciliation.vo.PaymentProofVo;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProofServiceImpl extends ServiceImpl<PaymentProofMapper, PaymentProof> implements PaymentProofService {

    private final PaymentProofMapper paymentProofMapper;
    private final InvoiceMapper invoiceMapper;
    private final CurrentMerchantProvider currentMerchantProvider;
    private final ObjectMapper objectMapper;

    public PaymentProofServiceImpl(
        PaymentProofMapper paymentProofMapper,
        InvoiceMapper invoiceMapper,
        CurrentMerchantProvider currentMerchantProvider,
        ObjectMapper objectMapper
    ) {
        this.paymentProofMapper = paymentProofMapper;
        this.invoiceMapper = invoiceMapper;
        this.currentMerchantProvider = currentMerchantProvider;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Transactional
    @Override
    public boolean saveIfAbsent(
        Invoice invoice,
        PaymentTransaction paymentTransaction,
        ReconciliationRecord reconciliationRecord,
        InvoiceStatusEnum finalStatus,
        String exceptionTags,
        OffsetDateTime paidAt
    ) {
        if (invoice == null || invoice.getId() == null || paymentTransaction == null || paymentTransaction.getTxHash() == null) {
            return false;
        }
        if (existsByInvoiceIdAndTxHash(invoice.getId(), paymentTransaction.getTxHash())) {
            return false;
        }

        PaymentProof paymentProof = new PaymentProof();
        paymentProof.setInvoiceId(invoice.getId());
        paymentProof.setTxHash(paymentTransaction.getTxHash());
        paymentProof.setProofType(PaymentProofTypeEnum.INVOICE_PAYMENT_RESULT);
        paymentProof.setProofPayload(
            objectMapper.valueToTree(
                new PaymentProofPayload(
                    invoice.getId(),
                    invoice.getPublicId(),
                    invoice.getInvoiceNo(),
                    paymentTransaction.getTxHash(),
                    paymentTransaction.getReferenceKey(),
                    paymentTransaction.getPayerAddress(),
                    paymentTransaction.getRecipientAddress(),
                    paymentTransaction.getMintAddress(),
                    paymentTransaction.getAmount(),
                    paidAt,
                    paymentTransaction.getVerificationResult(),
                    finalStatus,
                    splitExceptionTags(exceptionTags),
                    reconciliationRecord == null ? null : reconciliationRecord.getReconciliationStatus(),
                    reconciliationRecord == null ? null : reconciliationRecord.getResultMessage()
                )
            )
        );
        paymentProofMapper.insert(paymentProof);
        return true;
    }

    @Override
    public PaymentProofVo getLatestProof(Long invoiceId) {
        getOwnedInvoice(invoiceId);

        PaymentProof paymentProof = paymentProofMapper.selectOne(
            new LambdaQueryWrapper<PaymentProof>()
                .eq(PaymentProof::getInvoiceId, invoiceId)
                .orderByDesc(PaymentProof::getCreatedAt)
                .orderByDesc(PaymentProof::getId)
                .last("LIMIT 1")
        );
        if (paymentProof == null || paymentProof.getProofPayload() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_PROOF_NOT_FOUND);
        }

        PaymentProofPayload payload = objectMapper.convertValue(paymentProof.getProofPayload(), PaymentProofPayload.class);
        return new PaymentProofVo(
            payload.invoiceId(),
            payload.publicId(),
            payload.invoiceNo(),
            payload.txHash(),
            payload.referenceKey(),
            payload.payerAddress(),
            payload.recipientAddress(),
            payload.mintAddress(),
            payload.amount(),
            payload.paidAt(),
            payload.verificationResult(),
            payload.finalStatus(),
            payload.exceptionTags(),
            payload.reconciliationStatus(),
            payload.resultMessage(),
            paymentProof.getCreatedAt()
        );
    }

    private boolean existsByInvoiceIdAndTxHash(Long invoiceId, String txHash) {
        return paymentProofMapper.selectCount(
            new LambdaQueryWrapper<PaymentProof>()
                .eq(PaymentProof::getInvoiceId, invoiceId)
                .eq(PaymentProof::getTxHash, txHash)
        ) > 0;
    }

    private Invoice getOwnedInvoice(Long invoiceId) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null || !merchantId.equals(invoice.getMerchantId())) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND);
        }
        return invoice;
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

    private record PaymentProofPayload(
        /** Invoice id / 账单 ID */
        Long invoiceId,
        /** Public invoice id / 公开账单标识 */
        String publicId,
        /** Invoice number / 账单编号 */
        String invoiceNo,
        /** Blockchain transaction hash / 链上交易哈希 */
        String txHash,
        /** Invoice reference key / 账单 reference */
        String referenceKey,
        /** Payer wallet address / 付款地址 */
        String payerAddress,
        /** Recipient wallet address / 收款地址 */
        String recipientAddress,
        /** Token mint address / 代币 Mint 地址 */
        String mintAddress,
        /** Paid amount / 支付金额 */
        BigDecimal amount,
        /** Paid time in UTC / 支付时间（UTC） */
        OffsetDateTime paidAt,
        /** Payment verification result / 支付验证结果 */
        PaymentVerificationResultEnum verificationResult,
        /** Final invoice status / 最终账单状态 */
        InvoiceStatusEnum finalStatus,
        /** Exception tags / 异常标签 */
        List<String> exceptionTags,
        /** Reconciliation status / 核销状态 */
        com.stableflow.reconciliation.enums.ReconciliationStatusEnum reconciliationStatus,
        /** Reconciliation result message / 核销结果说明 */
        String resultMessage
    ) {
    }
}
