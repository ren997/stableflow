package com.stableflow.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.dto.MerchantWalletOwnershipVerifyRequestDto;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipChallengeVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipVerifyVo;
import java.util.List;

public interface MerchantPaymentConfigService extends IService<MerchantPaymentConfig> {

    /** Create or update the current merchant payment configuration / 创建或更新当前商家的收款配置 */
    MerchantPaymentConfigVo saveOrUpdate(MerchantPaymentConfigRequestDto request);

    /** Return the current merchant active payment configuration / 返回当前商家的启用收款配置 */
    MerchantPaymentConfigVo getCurrentConfig();

    /** Create a wallet ownership challenge for the current merchant payment config / 为当前商家收款配置创建钱包地址所有权挑战 */
    MerchantWalletOwnershipChallengeVo createOwnershipChallenge();

    /** Submit wallet ownership signature for later verification or future verifier integration / 提交钱包地址所有权签名，供当前留痕与后续验签扩展使用 */
    MerchantWalletOwnershipVerifyVo verifyOwnership(MerchantWalletOwnershipVerifyRequestDto request);

    /** Return the active payment configuration of a specific merchant / 返回指定商家的启用收款配置 */
    MerchantPaymentConfig getRequiredConfig(Long merchantId);

    /** List all active merchant payment configurations for scanning / 列出所有启用中的商家收款配置 */
    List<MerchantPaymentConfig> listActiveConfigs();
}
