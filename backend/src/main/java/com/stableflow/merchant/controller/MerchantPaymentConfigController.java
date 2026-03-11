package com.stableflow.merchant.controller;

import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.system.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/payment-config")
public class MerchantPaymentConfigController {

    private final MerchantPaymentConfigService merchantPaymentConfigService;

    public MerchantPaymentConfigController(MerchantPaymentConfigService merchantPaymentConfigService) {
        this.merchantPaymentConfigService = merchantPaymentConfigService;
    }

    @PostMapping
    public ApiResponse<MerchantPaymentConfigVo> saveOrUpdate(@Valid @RequestBody MerchantPaymentConfigRequestDto request) {
        return ApiResponse.success(merchantPaymentConfigService.saveOrUpdate(request));
    }

    @GetMapping
    public ApiResponse<MerchantPaymentConfigVo> getCurrentConfig() {
        return ApiResponse.success(merchantPaymentConfigService.getCurrentConfig());
    }
}
