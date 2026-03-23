package com.stableflow.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.reconciliation.service.PaymentProofService;
import com.stableflow.reconciliation.service.ReconciliationService;
import com.stableflow.system.api.ApiResponse;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import com.stableflow.verification.service.PaymentVerificationService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MainFlowIntegrationTest {

    private static final EmbeddedPostgres EMBEDDED_POSTGRES = startEmbeddedPostgres();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private PaymentVerificationService paymentVerificationService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private InvoicePaymentRequestMapper invoicePaymentRequestMapper;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EMBEDDED_POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void closeEmbeddedPostgres() throws IOException {
        EMBEDDED_POSTGRES.close();
    }

    @Test
    void shouldLoginConfigureAndCreateInvoice() throws Exception {
        MerchantSession merchantSession = registerAndLogin();

        savePaymentConfig(merchantSession.token());

        JsonNode createResponse = createInvoice(
            merchantSession.token(),
            new BigDecimal("99.00"),
            OffsetDateTime.now().plusDays(1)
        );

        assertEquals("DRAFT", createResponse.at("/data/status").asText());
        assertTrue(createResponse.at("/data/paymentInfo").isMissingNode() || createResponse.at("/data/paymentInfo").isNull());
        assertTrue(createResponse.at("/data/id").asLong() > 0);
    }

    @Test
    void shouldCreateActivateVerifyAndReconcileInvoiceEndToEnd() throws Exception {
        MerchantSession merchantSession = registerAndLogin();
        savePaymentConfig(merchantSession.token());

        JsonNode createResponse = createInvoice(
            merchantSession.token(),
            new BigDecimal("99.00"),
            OffsetDateTime.now().plusDays(1)
        );
        long invoiceId = createResponse.at("/data/id").asLong();

        JsonNode activateResponse = activateInvoice(merchantSession.token(), invoiceId);
        String referenceKey = activateResponse.at("/data/paymentInfo/referenceKey").asText();
        assertEquals("PENDING", activateResponse.at("/data/status").asText());
        assertFalse(referenceKey.isBlank());

        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTxHash("tx-" + UUID.randomUUID());
        paymentTransaction.setReferenceKey(referenceKey);
        paymentTransaction.setPayerAddress("payer-" + UUID.randomUUID());
        paymentTransaction.setRecipientAddress("wallet-" + UUID.randomUUID());
        paymentTransaction.setAmount(new BigDecimal("99.00"));
        paymentTransaction.setCurrency("USDC");
        paymentTransaction.setMintAddress("mint-usdc-test");
        paymentTransaction.setBlockTime(OffsetDateTime.now());
        paymentTransaction.setVerificationResult(PaymentVerificationResultEnum.PENDING);
        assertTrue(paymentTransactionService.saveIfAbsent(paymentTransaction));

        assertEquals(1, paymentVerificationService.verifyPendingTransactions(10));
        assertEquals(1, reconciliationService.reconcilePendingTransactions(10));

        JsonNode paymentStatusResponse = postWithBearer(
            merchantSession.token(),
            "/api/invoices/payment-status",
            "{\"id\":%d}".formatted(invoiceId)
        );
        assertEquals("PAID", paymentStatusResponse.at("/data/status").asText());
        assertEquals("PAID", paymentStatusResponse.at("/data/latestVerificationResult").asText());

        JsonNode paymentProofResponse = postWithBearer(
            merchantSession.token(),
            "/api/invoices/payment-proof",
            "{\"id\":%d}".formatted(invoiceId)
        );
        assertEquals("PAID", paymentProofResponse.at("/data/finalStatus").asText());
        assertEquals(paymentTransaction.getTxHash(), paymentProofResponse.at("/data/txHash").asText());
    }

    @Test
    void shouldVerifyExceptionScenariosAndDeduplicateDuplicateTxHash() throws Exception {
        MerchantSession merchantSession = registerAndLogin();
        savePaymentConfig(merchantSession.token());

        ActivatedInvoice partialInvoice = createAndActivateInvoice(merchantSession.token(), new BigDecimal("100.00"), OffsetDateTime.now().plusDays(1));
        ActivatedInvoice overpaidInvoice = createAndActivateInvoice(merchantSession.token(), new BigDecimal("100.00"), OffsetDateTime.now().plusDays(1));
        ActivatedInvoice lateInvoice = createAndActivateInvoice(merchantSession.token(), new BigDecimal("100.00"), OffsetDateTime.now().plusMinutes(30));

        PaymentTransaction partialTransaction = persistCandidateTransaction(
            "tx-partial-" + UUID.randomUUID(),
            partialInvoice.referenceKey(),
            new BigDecimal("80.00"),
            OffsetDateTime.now()
        );
        PaymentTransaction overpaidTransaction = persistCandidateTransaction(
            "tx-overpaid-" + UUID.randomUUID(),
            overpaidInvoice.referenceKey(),
            new BigDecimal("120.00"),
            OffsetDateTime.now()
        );
        PaymentTransaction lateTransaction = persistCandidateTransaction(
            "tx-late-" + UUID.randomUUID(),
            lateInvoice.referenceKey(),
            new BigDecimal("100.00"),
            OffsetDateTime.now().plusHours(2)
        );
        PaymentTransaction missingReferenceTransaction = persistCandidateTransaction(
            "tx-missing-reference-" + UUID.randomUUID(),
            null,
            new BigDecimal("50.00"),
            OffsetDateTime.now()
        );
        PaymentTransaction invalidReferenceTransaction = persistCandidateTransaction(
            "tx-invalid-reference-" + UUID.randomUUID(),
            "ref-invalid-" + UUID.randomUUID(),
            new BigDecimal("50.00"),
            OffsetDateTime.now()
        );

        assertEquals(5, paymentVerificationService.verifyPendingTransactions(10));

        assertEquals(
            PaymentVerificationResultEnum.PARTIALLY_PAID,
            paymentTransactionService.getById(partialTransaction.getId()).getVerificationResult()
        );
        assertEquals(
            PaymentVerificationResultEnum.OVERPAID,
            paymentTransactionService.getById(overpaidTransaction.getId()).getVerificationResult()
        );
        assertEquals(
            PaymentVerificationResultEnum.LATE_PAYMENT,
            paymentTransactionService.getById(lateTransaction.getId()).getVerificationResult()
        );
        assertEquals(
            PaymentVerificationResultEnum.MISSING_REFERENCE,
            paymentTransactionService.getById(missingReferenceTransaction.getId()).getVerificationResult()
        );
        assertEquals(
            PaymentVerificationResultEnum.INVALID_REFERENCE,
            paymentTransactionService.getById(invalidReferenceTransaction.getId()).getVerificationResult()
        );

        PaymentTransaction duplicateTransaction = new PaymentTransaction();
        duplicateTransaction.setTxHash(partialTransaction.getTxHash());
        duplicateTransaction.setReferenceKey(partialInvoice.referenceKey());
        duplicateTransaction.setPayerAddress("payer-duplicate");
        duplicateTransaction.setRecipientAddress("wallet-duplicate");
        duplicateTransaction.setAmount(new BigDecimal("80.00"));
        duplicateTransaction.setCurrency("USDC");
        duplicateTransaction.setMintAddress("mint-usdc-test");
        duplicateTransaction.setBlockTime(OffsetDateTime.now());
        duplicateTransaction.setVerificationResult(PaymentVerificationResultEnum.PENDING);

        assertFalse(paymentTransactionService.saveIfAbsent(duplicateTransaction));
    }

    private MerchantSession registerAndLogin() throws Exception {
        String email = "merchant+" + UUID.randomUUID() + "@example.com";
        String password = "Password123";
        String merchantName = "Merchant " + UUID.randomUUID();

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        java.util.Map.of(
                            "merchantName", merchantName,
                            "email", email,
                            "password", password
                        )
                    )
                )
        ).andExpect(status().isOk());

        JsonNode loginResponse = postJson(
            "/api/auth/login",
            objectMapper.writeValueAsString(
                java.util.Map.of(
                    "email", email,
                    "password", password
                )
            )
        );

        return new MerchantSession(
            email,
            password,
            loginResponse.at("/data/accessToken").asText(),
            loginResponse.at("/data/merchantId").asLong()
        );
    }

    private void savePaymentConfig(String token) throws Exception {
        postWithBearer(
            token,
            "/api/merchant/payment-config",
            """
            {
              "walletAddress":"wallet-test",
              "mintAddress":"mint-usdc-test",
              "chain":"SOLANA"
            }
            """
        );
    }

    private JsonNode createInvoice(String token, BigDecimal amount, OffsetDateTime expireAt) throws Exception {
        return postWithBearer(
            token,
            "/api/invoices",
            """
            {
              "customerName":"Alice",
              "amount":%s,
              "currency":"USDC",
              "chain":"SOLANA",
              "description":"Integration test invoice",
              "expireAt":"%s"
            }
            """.formatted(amount.toPlainString(), expireAt.toString())
        );
    }

    private JsonNode activateInvoice(String token, long invoiceId) throws Exception {
        return postWithBearer(token, "/api/invoices/activate", "{\"id\":%d}".formatted(invoiceId));
    }

    private ActivatedInvoice createAndActivateInvoice(String token, BigDecimal amount, OffsetDateTime expireAt) throws Exception {
        JsonNode createResponse = createInvoice(token, amount, expireAt);
        long invoiceId = createResponse.at("/data/id").asLong();
        JsonNode activateResponse = activateInvoice(token, invoiceId);
        InvoicePaymentRequest paymentRequest = invoicePaymentRequestMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InvoicePaymentRequest>()
                .eq(InvoicePaymentRequest::getInvoiceId, invoiceId)
        );
        return new ActivatedInvoice(invoiceId, activateResponse.at("/data/publicId").asText(), paymentRequest.getReferenceKey());
    }

    private PaymentTransaction persistCandidateTransaction(
        String txHash,
        String referenceKey,
        BigDecimal amount,
        OffsetDateTime blockTime
    ) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTxHash(txHash);
        paymentTransaction.setReferenceKey(referenceKey);
        paymentTransaction.setPayerAddress("payer-" + UUID.randomUUID());
        paymentTransaction.setRecipientAddress("wallet-test");
        paymentTransaction.setAmount(amount);
        paymentTransaction.setCurrency("USDC");
        paymentTransaction.setMintAddress("mint-usdc-test");
        paymentTransaction.setBlockTime(blockTime);
        paymentTransaction.setVerificationResult(PaymentVerificationResultEnum.PENDING);
        assertTrue(paymentTransactionService.saveIfAbsent(paymentTransaction));
        assertNotNull(paymentTransaction.getId());
        return paymentTransaction;
    }

    private JsonNode postJson(String path, String body) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    private JsonNode postWithBearer(String token, String path, String body) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
            post(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    private record MerchantSession(String email, String password, String token, Long merchantId) {
    }

    private record ActivatedInvoice(Long invoiceId, String publicId, String referenceKey) {
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().setPort(0).start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start embedded PostgreSQL for integration tests", exception);
        }
    }
}
