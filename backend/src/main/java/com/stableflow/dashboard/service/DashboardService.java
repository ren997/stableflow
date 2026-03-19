package com.stableflow.dashboard.service;

import com.stableflow.dashboard.vo.DashboardSummaryVo;

public interface DashboardService {

    /** Return the dashboard summary for the current merchant / 返回当前商家的 Dashboard 汇总数据 */
    DashboardSummaryVo getSummary();
}
