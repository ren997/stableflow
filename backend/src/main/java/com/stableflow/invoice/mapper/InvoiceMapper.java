package com.stableflow.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.invoice.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
