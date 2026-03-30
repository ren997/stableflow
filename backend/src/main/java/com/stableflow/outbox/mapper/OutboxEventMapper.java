package com.stableflow.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stableflow.outbox.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
