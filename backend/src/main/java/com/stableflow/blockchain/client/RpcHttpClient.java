package com.stableflow.blockchain.client;

import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RpcHttpClient {

    private static final Logger log = LoggerFactory.getLogger(RpcHttpClient.class);
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMillis(500);

    private final RestClient restClient;
    private final int retryMaxAttempts;
    private final Duration retryDelay;

    @Autowired
    public RpcHttpClient(SolanaProperties solanaProperties) {
        this(
            buildRestClient(solanaProperties),
            solanaProperties.retryMaxAttempts(),
            solanaProperties.retryDelay()
        );
    }

    RpcHttpClient(RestClient restClient, Integer retryMaxAttempts, Duration retryDelay) {
        this.restClient = restClient;
        this.retryMaxAttempts = retryMaxAttempts == null || retryMaxAttempts <= 0
            ? DEFAULT_RETRY_MAX_ATTEMPTS
            : retryMaxAttempts;
        this.retryDelay = retryDelay == null || retryDelay.isNegative()
            ? DEFAULT_RETRY_DELAY
            : retryDelay;
    }

    private static RestClient buildRestClient(SolanaProperties solanaProperties) {
        Duration connectTimeout = solanaProperties.connectTimeout() == null
            ? Duration.ofSeconds(3)
            : solanaProperties.connectTimeout();
        Duration readTimeout = solanaProperties.readTimeout() == null
            ? Duration.ofSeconds(10)
            : solanaProperties.readTimeout();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
            .baseUrl(solanaProperties.rpcUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(requestFactory)
            .build();
    }

    public <T> T call(
        SolanaRpcRequestDto request,
        ParameterizedTypeReference<JsonRpcResponseDto<T>> responseType
    ) {
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                return doCall(request, responseType);
            } catch (ResourceAccessException ex) {
                BusinessException businessException = buildResourceAccessException(request, ex);
                if (!shouldRetryResourceAccess(ex) || attempt >= retryMaxAttempts) {
                    logRetryExhausted(request.method(), attempt, businessException);
                    throw businessException;
                }
                logRetrying(request.method(), attempt, businessException);
                sleepBeforeRetry();
            } catch (RestClientResponseException ex) {
                BusinessException businessException = buildHttpResponseException(request, ex);
                if (!shouldRetryHttpStatus(ex.getStatusCode()) || attempt >= retryMaxAttempts) {
                    logRetryExhausted(request.method(), attempt, businessException);
                    throw businessException;
                }
                logRetrying(request.method(), attempt, businessException);
                sleepBeforeRetry();
            } catch (RestClientException ex) {
                BusinessException businessException = buildRestClientException(request, ex);
                logRetryExhausted(request.method(), attempt, businessException);
                throw businessException;
            }
        }
        throw new BusinessException(
            ErrorCode.SYSTEM_ERROR,
            "Unexpected Solana RPC retry flow termination for method: " + request.method()
        );
    }

    private <T> T doCall(
        SolanaRpcRequestDto request,
        ParameterizedTypeReference<JsonRpcResponseDto<T>> responseType
    ) {
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
    }

    private BusinessException buildResourceAccessException(SolanaRpcRequestDto request, ResourceAccessException ex) {
        if (isTimeoutException(ex)) {
            return new BusinessException(
                ErrorCode.BLOCKCHAIN_RPC_TIMEOUT,
                "Solana RPC timeout for method: " + request.method()
            );
        }
        return new BusinessException(
            ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
            "Solana RPC access failure for method %s: %s".formatted(request.method(), ex.getMessage())
        );
    }

    private BusinessException buildHttpResponseException(SolanaRpcRequestDto request, RestClientResponseException ex) {
        return new BusinessException(
            ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
            "Solana RPC HTTP error for method %s: status=%s, response=%s".formatted(
                request.method(),
                ex.getStatusCode(),
                ex.getResponseBodyAsString()
            )
        );
    }

    private BusinessException buildRestClientException(SolanaRpcRequestDto request, RestClientException ex) {
        return new BusinessException(
            ErrorCode.BLOCKCHAIN_RPC_HTTP_ERROR,
            "Solana RPC client error for method %s: %s".formatted(request.method(), ex.getMessage())
        );
    }

    private boolean shouldRetryResourceAccess(ResourceAccessException ex) {
        return isTimeoutException(ex) || ex.getCause() != null;
    }

    private boolean shouldRetryHttpStatus(HttpStatusCode statusCode) {
        return statusCode.value() == 429 || statusCode.is5xxServerError();
    }

    private void logRetrying(String method, int attempt, BusinessException businessException) {
        log.warn(
            "Solana RPC retrying method={}, attempt={}/{}, errorCode={}, message={}",
            method,
            attempt,
            retryMaxAttempts,
            businessException.getErrorCode(),
            businessException.getMessage()
        );
    }

    private void logRetryExhausted(String method, int attempt, BusinessException businessException) {
        log.error(
            "Solana RPC retry exhausted method={}, attempts={}, errorCode={}, message={}",
            method,
            attempt,
            businessException.getErrorCode(),
            businessException.getMessage()
        );
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Interrupted while waiting for Solana RPC retry");
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
