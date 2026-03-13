package com.stableflow.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.blockchain.entity.PaymentScanCursor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentScanCursorMapper extends BaseMapper<PaymentScanCursor> {
}
