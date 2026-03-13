package com.stableflow.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {
}
