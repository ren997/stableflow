package com.stableflow.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

/** Active fixed-address payment configuration owned by a merchant / 商家持有的固定收款地址配置实体 */
@Data
@TableName("merchant_payment_config")
public class MerchantPaymentConfig {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Related merchant id / 关联商家 ID */
    private Long merchantId;

    /** Fixed recipient wallet address / 固定收款钱包地址 */
    private String walletAddress;

    /** Token mint address / 代币 Mint 地址 */
    private String mintAddress;

    /** Blockchain network name / 区块链网络名称 */
    private String chain;

    /** Whether this config is active / 当前配置是否启用 */
    private Boolean activeFlag;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;

    /** Record updated time in UTC / 记录更新时间（UTC） */
    private OffsetDateTime updatedAt;
}
