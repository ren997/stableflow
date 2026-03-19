package com.stableflow.invoice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.dto.UpdateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.reconciliation.service.ReconciliationRecordService;
import com.stableflow.system.config.PaymentProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceUpdateTest {

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoicePaymentRequestMapper invoicePaymentRequestMapper;

    @Mock
    private MerchantPaymentConfigService merchantPaymentConfigService;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private ReconciliationRecordService reconciliationRecordService;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
            invoiceMapper,
            invoicePaymentRequestMapper,
            merchantPaymentConfigService,
            currentMerchantProvider,
            new PaymentProperties("fixed-address-reference", "https://stableflow.test"),
            paymentTransactionService,
            reconciliationRecordService
        );
    }

    @Test
    void shouldFullyUpdateDraftInvoiceAndPaymentRequest() {
        Invoice invoice = invoice(200L, 10L, InvoiceStatusEnum.DRAFT, new BigDecimal("99.00"), "USDC", "SOLANA");
        InvoicePaymentRequest paymentRequest = paymentRequest(200L, new BigDecimal("99.00"), utc("2026-03-22T10:00:00Z"));
        UpdateInvoiceRequestDto request = new UpdateInvoiceRequestDto(
            200L,
            "Bob",
            new BigDecimal("120.50"),
            "usdc",
            "solana",
            "Updated draft",
            utc("2026-03-23T10:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(200L)).thenReturn(invoice);
        when(invoicePaymentRequestMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(paymentRequest);

        InvoiceDetailVo response = invoiceService.updateInvoice(request);

        assertEquals("Bob", invoice.getCustomerName());
        assertEquals(new BigDecimal("120.50"), invoice.getAmount());
        assertEquals("USDC", invoice.getCurrency());
        assertEquals("SOLANA", invoice.getChain());
        assertEquals("Updated draft", invoice.getDescription());
        assertEquals(utc("2026-03-23T10:00:00Z"), invoice.getExpireAt());
        assertEquals(new BigDecimal("120.50"), paymentRequest.getExpectedAmount());
        assertEquals(utc("2026-03-23T10:00:00Z"), paymentRequest.getExpireAt());
        assertEquals("solana:wallet-1?amount=120.50&spl-token=mint-1&reference=ref-1&label=StableFlow+Invoice&message=INV-200", paymentRequest.getPaymentLink());
        assertEquals("Bob", response.customerName());
        assertEquals(new BigDecimal("120.50"), response.amount());

        verify(invoiceMapper).updateById(invoice);
        verify(invoicePaymentRequestMapper).updateById(paymentRequest);
    }

    @Test
    void shouldRejectPendingInvoiceUpdates() {
        Invoice invoice = invoice(201L, 10L, InvoiceStatusEnum.PENDING, new BigDecimal("99.00"), "USDC", "SOLANA");
        UpdateInvoiceRequestDto request = new UpdateInvoiceRequestDto(
            201L,
            "Charlie",
            new BigDecimal("99.00"),
            "USDC",
            "SOLANA",
            "Updated pending",
            utc("2026-03-24T10:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(201L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.updateInvoice(request));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Only DRAFT invoices can be edited", exception.getMessage());
    }

    @Test
    void shouldRejectNonEditableInvoiceStatuses() {
        Invoice invoice = invoice(203L, 10L, InvoiceStatusEnum.PENDING, new BigDecimal("99.00"), "USDC", "SOLANA");
        UpdateInvoiceRequestDto request = new UpdateInvoiceRequestDto(
            203L,
            "Alice",
            new BigDecimal("99.00"),
            "USDC",
            "SOLANA",
            "Updated paid",
            utc("2026-03-24T10:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(203L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.updateInvoice(request));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Only DRAFT invoices can be edited", exception.getMessage());
    }

    private Invoice invoice(
        Long id,
        Long merchantId,
        InvoiceStatusEnum status,
        BigDecimal amount,
        String currency,
        String chain
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setMerchantId(merchantId);
        invoice.setPublicId("pub_" + id);
        invoice.setInvoiceNo("INV-" + id);
        invoice.setCustomerName("Alice");
        invoice.setAmount(amount);
        invoice.setCurrency(currency);
        invoice.setChain(chain);
        invoice.setDescription("Initial description");
        invoice.setStatus(status);
        invoice.setExpireAt(utc("2026-03-22T10:00:00Z"));
        return invoice;
    }

    private InvoicePaymentRequest paymentRequest(Long invoiceId, BigDecimal expectedAmount, OffsetDateTime expireAt) {
        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setId(invoiceId);
        paymentRequest.setInvoiceId(invoiceId);
        paymentRequest.setRecipientAddress("wallet-1");
        paymentRequest.setReferenceKey("ref-1");
        paymentRequest.setMintAddress("mint-1");
        paymentRequest.setExpectedAmount(expectedAmount);
        paymentRequest.setLabel("StableFlow Invoice");
        paymentRequest.setMessage("INV-" + invoiceId);
        paymentRequest.setPaymentLink("solana:wallet-1");
        paymentRequest.setExpireAt(expireAt);
        return paymentRequest;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value);
    }
}
