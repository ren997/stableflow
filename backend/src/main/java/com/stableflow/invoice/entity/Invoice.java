package com.stableflow.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/** Merchant invoice aggregate that represents the payable business order / 表示商家应收业务订单的账单聚合实体 */
@Data
@TableName(value = "invoice", autoResultMap = true)
public class Invoice {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Public invoice identifier for external access / 对外访问使用的账单公开标识 */
    private String publicId;

    /** Merchant id owning the invoice / 账单所属商家 ID */
    private Long merchantId;

    /** Business invoice number / 业务账单编号 */
    private String invoiceNo;

    /** Customer display name / 客户展示名称 */
    private String customerName;

    /** Expected payable amount / 应付金额 */
    private BigDecimal amount;

    /** Invoice currency code / 账单币种代码 */
    private String currency;

    /** Blockchain network name / 区块链网络名称 */
    private String chain;

    /** Invoice description / 账单描述 */
    private String description;

    /** Current invoice status / 当前账单状态 */
    private InvoiceStatusEnum status;

    /** Exception tag code list stored as JSON array / 以 JSON 数组存储的异常标签编码列表
     *  @see ExceptionTagEnum#DESC
     */
    @TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.OTHER)
    private List<String> exceptionTags;

    /** Invoice expiry time in UTC / 账单过期时间（UTC） */
    private OffsetDateTime expireAt;

    /** Final paid time in UTC / 最终支付时间（UTC） */
    private OffsetDateTime paidAt;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;

    /** Record updated time in UTC / 记录更新时间（UTC） */
    private OffsetDateTime updatedAt;
}
