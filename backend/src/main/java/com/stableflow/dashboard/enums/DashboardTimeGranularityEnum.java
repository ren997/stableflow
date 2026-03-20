package com.stableflow.dashboard.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Dashboard time granularity enum for trend aggregation / 仪表盘趋势聚合时间粒度枚举 */
@Getter
public enum DashboardTimeGranularityEnum {
    /** Daily aggregation / 按日聚合 */
    DAY("DAY", "按日"),
    /** Weekly aggregation / 按周聚合 */
    WEEK("WEEK", "按周"),
    /** Monthly aggregation / 按月聚合 */
    MONTH("MONTH", "按月");

    public static final String DESC = "仪表盘时间粒度: DAY-按日, WEEK-按周, MONTH-按月";

    private static final Map<String, DashboardTimeGranularityEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(DashboardTimeGranularityEnum::getCode, Function.identity()));

    /** Granularity code / 粒度编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Granularity description / 粒度说明 */
    private final String desc;

    DashboardTimeGranularityEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by granularity code / 按粒度编码解析枚举 */
    public static DashboardTimeGranularityEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
