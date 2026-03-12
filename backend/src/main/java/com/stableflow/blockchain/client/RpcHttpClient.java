package com.stableflow.blockchain.client;

import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RpcHttpClient {

    private final RestClient restClient;

    public RpcHttpClient(SolanaProperties solanaProperties) {
        Duration connectTimeout = solanaProperties.connectTimeout() == null
            ? Duration.ofSeconds(3)
            : solanaProperties.connectTimeout();
        Duration readTimeout = solanaProperties.readTimeout() == null
            ? Duration.ofSeconds(10)
            : solanaProperties.readTimeout();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
            .baseUrl(solanaProperties.rpcUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(requestFactory)
            .build();
    }

    public <T> T call(
        SolanaRpcRequestDto request,
        ParameterizedTypeReference<JsonRpcResponseDto<T>> responseType
    ) {
        try {
            JsonRpcResponseDto<T> response = restClient.post()
                .body(request)
                .retrieve()
                .body(responseType);

            if (response == null) {
                throw new BusinessException(
                    ErrorCode.BLOCKCHAIN_RPC_EMPTY_RESPONSE,
                    "Solana RPC returned empty response for method: " + request.method()
                );
            }
            if (response.getError() != null) {
                throw new BusinessException(
                    ErrorCode.BLOCKCHAIN_RPC_ERROR,
                    "Solana RPC error for method %s: %s %s".formatted(
                        request.method(),
                        response.getError().getCode(),
                        response.getError().getMessage()
                    )
                );
            }
            return response.getResult();
        } catch (ResourceAccessException ex) {
            if (isTimeoutException(ex)) {
                throw new BusinessException(
                    ErrorCode.BLOCKCHAIN_RPC_TIMEOUT,
                    "Solana RPC timeout for method: " + request.method()
                );
            }
            throw new BusinessException(
                ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
                "Solana RPC access failure for method %s: %s".formatted(request.method(), ex.getMessage())
            );
        } catch (RestClientResponseException ex) {
            throw new BusinessException(
                ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
                "Solana RPC HTTP error for method %s: status=%s, response=%s".formatted(
                    request.method(),
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
                )
            );
        } catch (RestClientException ex) {
            throw new BusinessException(
                ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
                "Solana RPC client error for method %s: %s".formatted(request.method(), ex.getMessage())
            );
        }
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                || current instanceof HttpTimeoutException
                || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
