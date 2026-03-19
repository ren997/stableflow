package com.stableflow.merchant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.stableflow.merchant.dto.MerchantPaymentConfigQueryDto;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.system.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/payment-config")
@Tag(name = "Merchant", description = "Merchant payment configuration APIs / 商家收款配置接口")
@RequiredArgsConstructor
public class MerchantPaymentConfigController {

    private final MerchantPaymentConfigService merchantPaymentConfigService;

    @PostMapping
    @Operation(summary = "Create or update merchant payment config / 创建或更新商家收款配置")
    public ApiResponse<MerchantPaymentConfigVo> saveOrUpdate(@Valid @RequestBody MerchantPaymentConfigRequestDto request) {
        return ApiResponse.success(merchantPaymentConfigService.saveOrUpdate(request));
    }

    @PostMapping("/get")
    @Operation(summary = "Query current merchant payment config / 查询当前商家收款配置")
    public ApiResponse<MerchantPaymentConfigVo> getCurrentConfig(@Valid @RequestBody MerchantPaymentConfigQueryDto request) {
        return ApiResponse.success(merchantPaymentConfigService.getCurrentConfig());
    }
}
