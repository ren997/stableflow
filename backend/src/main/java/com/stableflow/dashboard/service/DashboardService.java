package com.stableflow.dashboard.service;

import com.stableflow.dashboard.vo.DashboardExceptionInvoiceVo;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.dashboard.enums.DashboardTimeGranularityEnum;
import com.stableflow.dashboard.vo.DashboardSummaryTrendVo;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.system.api.PageResult;

public interface DashboardService {

    /** Return the dashboard summary for the current merchant / 返回当前商家的 Dashboard 汇总数据 */
    DashboardSummaryVo getSummary();

    /** Return invoice status distribution for the current merchant / 返回当前商家的账单状态分布 */
    DashboardInvoiceStatusDistributionVo getInvoiceStatusDistribution();

    /** Return received amount trend of the current merchant grouped by time granularity / 按时间粒度返回当前商家的收款趋势 */
    DashboardSummaryTrendVo getSummaryTrend(DashboardTimeGranularityEnum granularity);

    /** Return exception invoices of the current merchant with optional exception-tag filtering / 返回当前商家的异常账单，可按异常标签过滤 */
    PageResult<DashboardExceptionInvoiceVo> getExceptionInvoices(ExceptionTagEnum exceptionTag, int page, int size);
}
