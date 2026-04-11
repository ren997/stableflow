package com.stableflow.invoice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.dto.ActivateInvoiceRequestDto;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.PublicPaymentPageVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
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
class InvoiceLifecycleServiceTest {

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
    void shouldCreateDraftInvoiceWithoutExposingPaymentInfo() {
        MerchantPaymentConfig paymentConfig = paymentConfig();
        CreateInvoiceRequestDto request = new CreateInvoiceRequestDto(
            "Alice",
            new BigDecimal("99.00"),
            "USDC",
            "SOLANA",
            "Monthly fee",
            utc("2026-03-25T10:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(merchantPaymentConfigService.getRequiredConfig(10L)).thenReturn(paymentConfig);
        doAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(300L);
            return 1;
        }).when(invoiceMapper).insert(any(Invoice.class));

        InvoiceDetailVo response = invoiceService.createInvoice(request);

        assertEquals(InvoiceStatusEnum.DRAFT, response.status());
        assertNull(response.paymentInfo());
        verify(invoiceMapper).insert(any(Invoice.class));
        verify(invoicePaymentRequestMapper).insert(any(InvoicePaymentRequest.class));
    }

    @Test
    void shouldActivateDraftInvoiceAndExposePaymentInfo() {
        Invoice invoice = draftInvoice(301L);
        InvoicePaymentRequest paymentRequest = paymentRequest(301L);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(301L)).thenReturn(invoice);
        when(merchantPaymentConfigService.getRequiredConfig(10L)).thenReturn(paymentConfig());
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest);

        InvoiceDetailVo response = invoiceService.activateInvoice(new ActivateInvoiceRequestDto(301L));

        assertEquals(InvoiceStatusEnum.PENDING, invoice.getStatus());
        assertEquals(InvoiceStatusEnum.PENDING, response.status());
        assertNotNull(response.paymentInfo());
        verify(invoiceMapper).updateById(invoice);
        verify(invoicePaymentRequestMapper).updateById(paymentRequest);
    }

    @Test
    void shouldRejectActivationWhenInvoiceIsNotDraft() {
        Invoice invoice = draftInvoice(302L);
        invoice.setStatus(InvoiceStatusEnum.PENDING);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(302L)).thenReturn(invoice);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> invoiceService.activateInvoice(new ActivateInvoiceRequestDto(302L))
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Only DRAFT invoices can be activated", exception.getMessage());
    }

    @Test
    void shouldRejectDraftPaymentInfoBeforeActivation() {
        Invoice invoice = draftInvoice(303L);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(303L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.getPaymentInfo(303L));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Inactive invoices do not expose payment info before activation or after cancellation", exception.getMessage());
    }

    @Test
    void shouldRejectDraftPublicPaymentPageBeforeActivation() {
        Invoice invoice = draftInvoice(304L);

        when(invoiceMapper.selectOne(any())).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.getPublicPaymentPage("pub_304"));

        assertEquals(ErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Public payment page is not available for inactive invoices", exception.getMessage());
    }

    @Test
    void shouldExposePublicPaymentPageAfterActivation() {
        Invoice invoice = draftInvoice(305L);
        invoice.setStatus(InvoiceStatusEnum.PENDING);
        InvoicePaymentRequest paymentRequest = paymentRequest(305L);

        when(invoiceMapper.selectOne(any())).thenReturn(invoice);
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest);

        PublicPaymentPageVo publicPaymentPage = invoiceService.getPublicPaymentPage("pub_305");

        assertEquals("pub_305", publicPaymentPage.publicId());
        assertEquals(InvoiceStatusEnum.PENDING, publicPaymentPage.status());
        assertEquals("ref-1", publicPaymentPage.paymentInfo().referenceKey());
    }

    @Test
    void shouldCancelPendingInvoiceAndHidePaymentInfo() {
        Invoice invoice = draftInvoice(306L);
        invoice.setStatus(InvoiceStatusEnum.PENDING);
        InvoicePaymentRequest paymentRequest = paymentRequest(306L);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(306L)).thenReturn(invoice);
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest);

        InvoiceDetailVo response = invoiceService.cancelInvoice(306L);

        assertEquals(InvoiceStatusEnum.CANCELLED, invoice.getStatus());
        assertEquals(InvoiceStatusEnum.CANCELLED, response.status());
        assertNull(response.paymentInfo());
        verify(invoiceMapper).updateById(invoice);
    }

    @Test
    void shouldRejectCancellationWhenInvoiceAlreadyPaid() {
        Invoice invoice = draftInvoice(307L);
        invoice.setStatus(InvoiceStatusEnum.PAID);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(307L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.cancelInvoice(307L));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Only DRAFT or PENDING invoices can be cancelled", exception.getMessage());
        verify(invoiceMapper, never()).updateById(any(Invoice.class));
    }

    @Test
    void shouldRejectCancelledPaymentInfoAccess() {
        Invoice invoice = draftInvoice(308L);
        invoice.setStatus(InvoiceStatusEnum.CANCELLED);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(308L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.getPaymentInfo(308L));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("Inactive invoices do not expose payment info before activation or after cancellation", exception.getMessage());
    }

    private Invoice draftInvoice(Long id) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setMerchantId(10L);
        invoice.setPublicId("pub_" + id);
        invoice.setInvoiceNo("INV-" + id);
        invoice.setCustomerName("Alice");
        invoice.setAmount(new BigDecimal("99.00"));
        invoice.setCurrency("USDC");
        invoice.setChain("SOLANA");
        invoice.setDescription("Monthly fee");
        invoice.setStatus(InvoiceStatusEnum.DRAFT);
        invoice.setExpireAt(utc("2026-03-25T10:00:00Z"));
        return invoice;
    }

    private InvoicePaymentRequest paymentRequest(Long invoiceId) {
        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setId(invoiceId);
        paymentRequest.setInvoiceId(invoiceId);
        paymentRequest.setRecipientAddress("wallet-1");
        paymentRequest.setReferenceKey("ref-1");
        paymentRequest.setMintAddress("mint-1");
        paymentRequest.setExpectedAmount(new BigDecimal("99.00"));
        paymentRequest.setLabel("StableFlow Invoice");
        paymentRequest.setMessage("INV-" + invoiceId);
        paymentRequest.setExpireAt(utc("2026-03-25T10:00:00Z"));
        paymentRequest.setPaymentLink("solana:wallet-1");
        return paymentRequest;
    }

    private MerchantPaymentConfig paymentConfig() {
        MerchantPaymentConfig config = new MerchantPaymentConfig();
        config.setMerchantId(10L);
        config.setWalletAddress("wallet-1");
        config.setMintAddress("mint-1");
        config.setChain("SOLANA");
        return config;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value);
    }
}
