package com.stableflow.dashboard.vo;

import com.stableflow.invoice.enums.InvoiceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Dashboard invoice status distribution response / 仪表盘账单状态分布返回 */
@Schema(name = "DashboardInvoiceStatusDistributionVo", description = "Dashboard invoice status distribution response / 仪表盘账单状态分布返回")
public record DashboardInvoiceStatusDistributionVo(
    @Schema(description = "Status count items / 状态数量列表")
    List<StatusCountItem> items
) {

    /** Invoice status count item / 账单状态数量项 */
    @Schema(name = "DashboardInvoiceStatusDistributionVo.StatusCountItem", description = "Invoice status count item / 账单状态数量项")
    public record StatusCountItem(
        @Schema(description = "Invoice status / 账单状态", implementation = InvoiceStatusEnum.class)
        InvoiceStatusEnum status,
        @Schema(description = "Invoice count of the status / 该状态账单数量", example = "12")
        long count
    ) {
    }
}
