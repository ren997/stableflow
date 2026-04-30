package com.stableflow.system.service;

import com.stableflow.system.vo.SystemRuntimeConfigVo;

/** Query runtime configuration needed by frontend pages / 查询前端页面所需的运行时配置 */
public interface SystemRuntimeConfigService {

    /** Return resolved runtime configuration for the current deployment / 返回当前部署环境下解析完成的运行时配置 */
    SystemRuntimeConfigVo getRuntimeConfig();
}
