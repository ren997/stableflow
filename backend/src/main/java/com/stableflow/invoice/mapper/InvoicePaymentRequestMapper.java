package com.stableflow.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoicePaymentRequestMapper extends BaseMapper<InvoicePaymentRequest> {
}
