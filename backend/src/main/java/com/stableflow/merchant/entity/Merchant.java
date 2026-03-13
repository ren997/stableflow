package com.stableflow.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

/** Merchant account entity used for authentication and ownership boundaries / 用于认证和业务归属边界的商家账户实体 */
@Data
@TableName("merchant")
public class Merchant {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Merchant display name / 商家名称 */
    private String merchantName;

    /** Login email / 登录邮箱 */
    private String email;

    /** Password hash / 密码哈希 */
    private String passwordHash;

    /** Merchant status / 商家状态 */
    private String status;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;

    /** Record updated time in UTC / 记录更新时间（UTC） */
    private OffsetDateTime updatedAt;
}
