package com.stableflow.invoice.controller;

import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.vo.PublicPaymentPageVo;
import com.stableflow.system.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
@Tag(name = "Public Payment", description = "Public payment page APIs / 公共支付页接口")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final InvoiceService invoiceService;

    @GetMapping("/{publicId}")
    @Operation(summary = "Get public payment page info / 获取公共支付页信息")
    public ApiResponse<PublicPaymentPageVo> getPublicPaymentPage(@PathVariable("publicId") String publicId) {
        return ApiResponse.success(invoiceService.getPublicPaymentPage(publicId));
    }
}
