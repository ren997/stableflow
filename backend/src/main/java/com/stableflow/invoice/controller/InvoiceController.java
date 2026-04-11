package com.stableflow.invoice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.stableflow.invoice.dto.ActivateInvoiceRequestDto;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.dto.InvoiceIdQueryDto;
import com.stableflow.invoice.dto.InvoiceListQueryDto;
import com.stableflow.invoice.dto.ReconcileInvoiceRequestDto;
import com.stableflow.invoice.dto.UpdateInvoiceRequestDto;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.reconciliation.service.PaymentProofService;
import com.stableflow.reconciliation.service.ReconciliationService;
import com.stableflow.reconciliation.vo.ReconcileInvoiceVo;
import com.stableflow.reconciliation.vo.PaymentProofVo;
import com.stableflow.system.api.ApiResponse;
import com.stableflow.system.api.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice", description = "Invoice APIs / 账单接口")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentProofService paymentProofService;
    private final ReconciliationService reconciliationService;

    @PostMapping
    @Operation(summary = "Create invoice / 创建账单")
    public ApiResponse<InvoiceDetailVo> createInvoice(@Valid @RequestBody CreateInvoiceRequestDto request) {
        return ApiResponse.success(invoiceService.createInvoice(request));
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate invoice / 激活账单")
    public ApiResponse<InvoiceDetailVo> activateInvoice(@Valid @RequestBody ActivateInvoiceRequestDto request) {
        return ApiResponse.success(invoiceService.activateInvoice(request));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel invoice / 取消账单")
    public ApiResponse<InvoiceDetailVo> cancelInvoice(@Valid @RequestBody InvoiceIdQueryDto request) {
        return ApiResponse.success(invoiceService.cancelInvoice(request.id()));
    }

    @PostMapping("/update")
    @Operation(summary = "Update invoice / 编辑账单")
    public ApiResponse<InvoiceDetailVo> updateInvoice(@Valid @RequestBody UpdateInvoiceRequestDto request) {
        return ApiResponse.success(invoiceService.updateInvoice(request));
    }

    @PostMapping("/list")
    @Operation(summary = "Query invoice list / 查询账单列表")
    public ApiResponse<PageResult<InvoiceListItemVo>> listInvoices(@Valid @RequestBody InvoiceListQueryDto request) {
        return ApiResponse.success(
            invoiceService.listInvoices(
                request.status(),
                request.exceptionTag(),
                request.page() == null ? 1 : request.page(),
                request.size() == null ? 20 : request.size()
            )
        );
    }

    @PostMapping("/detail")
    @Operation(summary = "Query invoice detail / 查询账单详情")
    public ApiResponse<InvoiceDetailVo> getInvoiceDetail(@Valid @RequestBody InvoiceIdQueryDto request) {
        return ApiResponse.success(invoiceService.getInvoiceDetail(request.id()));
    }

    @PostMapping("/payment-info")
    @Operation(summary = "Query invoice payment info / 查询账单支付信息")
    public ApiResponse<PaymentInfoVo> getPaymentInfo(@Valid @RequestBody InvoiceIdQueryDto request) {
        return ApiResponse.success(invoiceService.getPaymentInfo(request.id()));
    }

    @PostMapping("/payment-status")
    @Operation(summary = "Query invoice payment status / 查询账单支付状态")
    public ApiResponse<PaymentStatusVo> getPaymentStatus(@Valid @RequestBody InvoiceIdQueryDto request) {
        return ApiResponse.success(invoiceService.getPaymentStatus(request.id()));
    }

    @PostMapping("/payment-proof")
    @Operation(summary = "Query invoice payment proof / 查询账单支付凭证")
    public ApiResponse<PaymentProofVo> getPaymentProof(@Valid @RequestBody InvoiceIdQueryDto request) {
        return ApiResponse.success(paymentProofService.getLatestProof(request.id()));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Manual reconcile invoice / 手动触发账单核销")
    public ApiResponse<ReconcileInvoiceVo> reconcileInvoice(@Valid @RequestBody ReconcileInvoiceRequestDto request) {
        return ApiResponse.success(reconciliationService.reconcileInvoice(request.id()));
    }
}
