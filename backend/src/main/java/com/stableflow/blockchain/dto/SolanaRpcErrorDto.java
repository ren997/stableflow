package com.stableflow.blockchain.dto;

import lombok.Data;

@Data
public class SolanaRpcErrorDto {

    /** RPC error code / RPC 错误码 */
    private Integer code;

    /** RPC error message / RPC 错误消息 */
    private String message;
}
