package com.stableflow.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantName;
    private String email;
    private String passwordHash;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
