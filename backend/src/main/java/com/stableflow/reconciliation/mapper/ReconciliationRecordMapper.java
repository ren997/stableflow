package com.stableflow.reconciliation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReconciliationRecordMapper extends BaseMapper<ReconciliationRecord> {
}
