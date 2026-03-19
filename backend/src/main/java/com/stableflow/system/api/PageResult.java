package com.stableflow.system.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Generic paginated result wrapper / 通用分页结果包装
 *
 * @param records  current page records / 当前页记录列表
 * @param total    total record count / 总记录数
 * @param page     current page number (1-based) / 当前页码（从 1 开始）
 * @param size     page size / 每页条数
 */
@Schema(name = "PageResult", description = "Paginated result / 分页结果")
public record PageResult<T>(
    @Schema(description = "Current page records / 当前页记录列表")
    List<T> records,
    @Schema(description = "Total record count / 总记录数", example = "120")
    long total,
    @Schema(description = "Current page number (1-based) / 当前页码", example = "1")
    long page,
    @Schema(description = "Page size / 每页条数", example = "20")
    long size
) {
}
