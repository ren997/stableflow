package com.stableflow.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
