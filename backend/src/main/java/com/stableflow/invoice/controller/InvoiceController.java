package com.stableflow.invoice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.system.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice", description = "Invoice APIs / 账单接口")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(summary = "Create invoice / 创建账单")
    public ApiResponse<InvoiceDetailVo> createInvoice(@Valid @RequestBody CreateInvoiceRequestDto request) {
        return ApiResponse.success(invoiceService.createInvoice(request));
    }

    @GetMapping
    @Operation(summary = "List invoices / 查询账单列表")
    public ApiResponse<List<InvoiceListItemVo>> listInvoices(@RequestParam(required = false) String status) {
        return ApiResponse.success(invoiceService.listInvoices(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice detail / 获取账单详情")
    public ApiResponse<InvoiceDetailVo> getInvoiceDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(invoiceService.getInvoiceDetail(id));
    }

    @GetMapping("/{id}/payment-info")
    @Operation(summary = "Get invoice payment info / 获取账单支付信息")
    public ApiResponse<PaymentInfoVo> getPaymentInfo(@PathVariable("id") Long id) {
        return ApiResponse.success(invoiceService.getPaymentInfo(id));
    }
}
