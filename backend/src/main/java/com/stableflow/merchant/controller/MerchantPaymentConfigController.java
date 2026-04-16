package com.stableflow.merchant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.stableflow.merchant.dto.MerchantPaymentConfigQueryDto;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.dto.MerchantWalletOwnershipChallengeRequestDto;
import com.stableflow.merchant.dto.MerchantWalletOwnershipVerifyRequestDto;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipChallengeVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipVerifyVo;
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

    @PostMapping("/ownership/challenge")
    @Operation(summary = "Create wallet ownership challenge / 创建钱包地址所有权挑战")
    public ApiResponse<MerchantWalletOwnershipChallengeVo> createOwnershipChallenge(
        @Valid @RequestBody(required = false) MerchantWalletOwnershipChallengeRequestDto request
    ) {
        return ApiResponse.success(merchantPaymentConfigService.createOwnershipChallenge());
    }

    @PostMapping("/ownership/verify")
    @Operation(summary = "Submit wallet ownership signature / 提交钱包地址所有权签名")
    public ApiResponse<MerchantWalletOwnershipVerifyVo> verifyOwnership(
        @Valid @RequestBody MerchantWalletOwnershipVerifyRequestDto request
    ) {
        return ApiResponse.success(merchantPaymentConfigService.verifyOwnership(request));
    }
}
