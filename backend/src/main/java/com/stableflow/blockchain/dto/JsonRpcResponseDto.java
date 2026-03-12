package com.stableflow.blockchain.dto;

import lombok.Data;

@Data
public class JsonRpcResponseDto<T> {

    /** JSON-RPC protocol version / JSON-RPC 协议版本 */
    private String jsonrpc;

    /** Request id echoed by the server / 服务端回传的请求 ID */
    private Integer id;

    /** Method-specific result payload / 方法对应的结果载荷 */
    private T result;

    /** RPC error object / RPC 错误对象 */
    private SolanaRpcErrorDto error;
}
