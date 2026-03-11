package com.stableflow.invoice.controller;

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
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ApiResponse<InvoiceDetailVo> createInvoice(@Valid @RequestBody CreateInvoiceRequestDto request) {
        return ApiResponse.success(invoiceService.createInvoice(request));
    }

    @GetMapping
    public ApiResponse<List<InvoiceListItemVo>> listInvoices(@RequestParam(required = false) String status) {
        return ApiResponse.success(invoiceService.listInvoices(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceDetailVo> getInvoiceDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(invoiceService.getInvoiceDetail(id));
    }

    @GetMapping("/{id}/payment-info")
    public ApiResponse<PaymentInfoVo> getPaymentInfo(@PathVariable("id") Long id) {
        return ApiResponse.success(invoiceService.getPaymentInfo(id));
    }
}
