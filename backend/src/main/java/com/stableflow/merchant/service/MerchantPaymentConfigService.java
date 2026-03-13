package com.stableflow.merchant.service;

import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import java.util.List;

public interface MerchantPaymentConfigService {

    /** Create or update the current merchant payment configuration / 创建或更新当前商家的收款配置 */
    MerchantPaymentConfigVo saveOrUpdate(MerchantPaymentConfigRequestDto request);

    /** Return the current merchant active payment configuration / 返回当前商家的启用收款配置 */
    MerchantPaymentConfigVo getCurrentConfig();

    /** Return the active payment configuration of a specific merchant / 返回指定商家的启用收款配置 */
    MerchantPaymentConfig getRequiredConfig(Long merchantId);

    /** List all active merchant payment configurations for scanning / 列出所有启用中的商家收款配置 */
    List<MerchantPaymentConfig> listActiveConfigs();
}
