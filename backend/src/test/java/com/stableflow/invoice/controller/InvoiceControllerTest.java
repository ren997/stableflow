package com.stableflow.invoice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.invoice.dto.ActivateInvoiceRequestDto;
import com.stableflow.invoice.dto.InvoiceIdQueryDto;
import com.stableflow.invoice.dto.InvoiceListQueryDto;
import com.stableflow.invoice.dto.ReconcileInvoiceRequestDto;
import com.stableflow.invoice.dto.UpdateInvoiceRequestDto;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.reconciliation.service.PaymentProofService;
import com.stableflow.reconciliation.service.ReconciliationService;
import com.stableflow.reconciliation.vo.PaymentProofVo;
import com.stableflow.reconciliation.vo.ReconcileInvoiceVo;
import com.stableflow.system.api.PageResult;
import com.stableflow.system.exception.GlobalExceptionHandler;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PaymentProofService paymentProofService;

    @Mock
    private ReconciliationService reconciliationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new InvoiceController(invoiceService, paymentProofService, reconciliationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListInvoicesViaPost() throws Exception {
        when(invoiceService.listInvoices("PENDING", ExceptionTagEnum.LATE_PAYMENT, 2, 10)).thenReturn(
            new PageResult<>(List.of(invoiceListItemVo()), 1, 2, 10)
        );

        mockMvc.perform(
                post("/api/invoices/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceListQueryDto("PENDING", ExceptionTagEnum.LATE_PAYMENT, 2, 10)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].invoiceNo").value("INV-001"))
            .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void shouldUseDefaultPaginationWhenQueryDtoOmitsPageAndSize() throws Exception {
        when(invoiceService.listInvoices(null, null, 1, 20)).thenReturn(
            new PageResult<>(List.of(), 0, 1, 20)
        );

        mockMvc.perform(
                post("/api/invoices/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceListQueryDto(null, null, null, null)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void shouldQueryInvoiceDetailViaPost() throws Exception {
        when(invoiceService.getInvoiceDetail(1L)).thenReturn(invoiceDetailVo());

        mockMvc.perform(
                post("/api/invoices/detail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceIdQueryDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.invoiceNo").value("INV-001"));
    }

    @Test
    void shouldUpdateInvoiceViaPost() throws Exception {
        when(invoiceService.updateInvoice(org.mockito.ArgumentMatchers.any(UpdateInvoiceRequestDto.class))).thenReturn(invoiceDetailVo());

        mockMvc.perform(
                post("/api/invoices/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new UpdateInvoiceRequestDto(
                                1L,
                                "Alice",
                                new BigDecimal("99.00"),
                                "USDC",
                                "SOLANA",
                                "Monthly fee",
                                OffsetDateTime.parse("2026-03-21T10:00:00Z")
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.invoiceNo").value("INV-001"));
    }

    @Test
    void shouldRejectCreateInvoiceWhenDescriptionExceedsDatabaseLimit() throws Exception {
        mockMvc.perform(
                post("/api/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new com.stableflow.invoice.dto.CreateInvoiceRequestDto(
                                "Alice",
                                new BigDecimal("99.00"),
                                "USDC",
                                "SOLANA",
                                "x".repeat(513),
                                OffsetDateTime.parse("2026-03-21T10:00:00Z")
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void shouldReturnBadRequestWhenInvoiceAmountTypeIsInvalid() throws Exception {
        mockMvc.perform(
                post("/api/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "customerName": "Alice",
                          "amount": "not-a-number",
                          "currency": "USDC",
                          "chain": "SOLANA",
                          "description": "Monthly fee",
                          "expireAt": "2026-03-21T10:00:00Z"
                        }
                        """
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40002))
            .andExpect(jsonPath("$.message").value("Invalid value for field: amount"));
    }

    @Test
    void shouldActivateInvoiceViaPost() throws Exception {
        when(invoiceService.activateInvoice(org.mockito.ArgumentMatchers.any(ActivateInvoiceRequestDto.class))).thenReturn(invoiceDetailVo());

        mockMvc.perform(
                post("/api/invoices/activate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ActivateInvoiceRequestDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.invoiceNo").value("INV-001"));
    }

    @Test
    void shouldQueryPaymentInfoViaPost() throws Exception {
        when(invoiceService.getPaymentInfo(1L)).thenReturn(paymentInfoVo());

        mockMvc.perform(
                post("/api/invoices/payment-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceIdQueryDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.referenceKey").value("ref-001"));
    }

    @Test
    void shouldQueryPaymentStatusViaPost() throws Exception {
        when(invoiceService.getPaymentStatus(1L)).thenReturn(paymentStatusVo());

        mockMvc.perform(
                post("/api/invoices/payment-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceIdQueryDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.exceptionTags[0]").value("LATE_PAYMENT"));
    }

    @Test
    void shouldQueryPaymentProofViaPost() throws Exception {
        when(paymentProofService.getLatestProof(1L)).thenReturn(paymentProofVo());

        mockMvc.perform(
                post("/api/invoices/payment-proof")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new InvoiceIdQueryDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.txHash").value("tx-001"))
            .andExpect(jsonPath("$.data.exceptionTags[0]").value("LATE_PAYMENT"));
    }

    @Test
    void shouldTriggerManualReconcileViaPost() throws Exception {
        when(reconciliationService.reconcileInvoice(1L)).thenReturn(
            new ReconcileInvoiceVo(1L, 1, paymentStatusVo())
        );

        mockMvc.perform(
                post("/api/invoices/reconcile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ReconcileInvoiceRequestDto(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.invoiceId").value(1L))
            .andExpect(jsonPath("$.data.reconciledCount").value(1))
            .andExpect(jsonPath("$.data.paymentStatus.status").value("PAID"));
    }

    @Test
    void shouldNotExposeLegacyGetInvoiceRoutes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/invoices"))
            .andExpect(status().isMethodNotAllowed());
    }

    private InvoiceListItemVo invoiceListItemVo() {
        return new InvoiceListItemVo(
            1L,
            "pub-001",
            "INV-001",
            "Alice",
            new BigDecimal("99.00"),
            "USDC",
            InvoiceStatusEnum.PENDING,
            OffsetDateTime.parse("2026-03-21T10:00:00Z"),
            OffsetDateTime.parse("2026-03-20T10:00:00Z")
        );
    }

    private InvoiceDetailVo invoiceDetailVo() {
        return new InvoiceDetailVo(
            1L,
            "pub-001",
            "INV-001",
            "Alice",
            new BigDecimal("99.00"),
            "USDC",
            "SOLANA",
            "Monthly fee",
            InvoiceStatusEnum.PENDING,
            OffsetDateTime.parse("2026-03-21T10:00:00Z"),
            null,
            OffsetDateTime.parse("2026-03-20T10:00:00Z"),
            paymentInfoVo()
        );
    }

    private PaymentInfoVo paymentInfoVo() {
        return new PaymentInfoVo(
            "wallet-1",
            "ref-001",
            "mint-1",
            new BigDecimal("99.00"),
            "solana:wallet-1",
            "StableFlow Invoice",
            "INV-001",
            OffsetDateTime.parse("2026-03-21T10:00:00Z")
        );
    }

    private PaymentStatusVo paymentStatusVo() {
        return new PaymentStatusVo(
            1L,
            "pub-001",
            "INV-001",
            InvoiceStatusEnum.PAID,
            List.of(ExceptionTagEnum.LATE_PAYMENT),
            OffsetDateTime.parse("2026-03-20T11:00:00Z"),
            OffsetDateTime.parse("2026-03-20T11:05:00Z"),
            "tx-001",
            PaymentVerificationResultEnum.PAID,
            PaymentTransactionStatusEnum.PAID
        );
    }

    private PaymentProofVo paymentProofVo() {
        return new PaymentProofVo(
            1L,
            "pub-001",
            "INV-001",
            "tx-001",
            "ref-001",
            "payer-1",
            "wallet-1",
            "mint-1",
            new BigDecimal("99.00"),
            OffsetDateTime.parse("2026-03-20T11:00:00Z"),
            PaymentVerificationResultEnum.PAID,
            InvoiceStatusEnum.PAID,
            List.of(ExceptionTagEnum.LATE_PAYMENT),
            com.stableflow.reconciliation.enums.ReconciliationStatusEnum.SUCCESS,
            "Invoice marked as paid.",
            OffsetDateTime.parse("2026-03-20T11:05:00Z")
        );
    }
}
