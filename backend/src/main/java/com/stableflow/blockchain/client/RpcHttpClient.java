package com.stableflow.blockchain.client;

import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RpcHttpClient {

    private final RestClient restClient;

    public RpcHttpClient(SolanaProperties solanaProperties) {
        this.restClient = RestClient.builder()
            .baseUrl(solanaProperties.rpcUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public <T> T call(
        SolanaRpcRequestDto request,
        ParameterizedTypeReference<JsonRpcResponseDto<T>> responseType
    ) {
        JsonRpcResponseDto<T> response = restClient.post()
            .body(request)
            .retrieve()
            .body(responseType);

        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Solana RPC returned empty response");
        }
        if (response.getError() != null) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Solana RPC error: " + response.getError().getCode() + " " + response.getError().getMessage()
            );
        }
        return response.getResult();
    }
}
