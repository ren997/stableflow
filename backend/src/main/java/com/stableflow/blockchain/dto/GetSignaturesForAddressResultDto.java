package com.stableflow.blockchain.dto;

import lombok.Data;

@Data
public class GetSignaturesForAddressResultDto {

    /** Transaction signature / 交易签名 */
    private String signature;

    /** Slot number / 槽位号 */
    private Long slot;

    /** Block time in epoch seconds / 区块时间秒级时间戳 */
    private Long blockTime;

    /** Raw execution error payload / 原始执行错误信息 */
    private Object err;
}
