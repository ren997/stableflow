package com.stableflow.blockchain.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith(MockitoExtension.class)
class RpcHttpClientTest {

    private static final ParameterizedTypeReference<JsonRpcResponseDto<String>> STRING_RESPONSE =
        new ParameterizedTypeReference<>() {};

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RpcHttpClient rpcHttpClient;

    @BeforeEach
    void setUp() {
        rpcHttpClient = new RpcHttpClient(restClient, 3, Duration.ZERO);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(SolanaRpcRequestDto.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void shouldRetryTimeoutAndSucceedWithinMaxAttempts() {
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
            .thenThrow(new ResourceAccessException("timeout", new SocketTimeoutException("read timed out")))
            .thenReturn(successResponse("ok"));

        String result = rpcHttpClient.call(request("getHealth"), STRING_RESPONSE);

        assertEquals("ok", result);
        verify(responseSpec, times(2)).body(any(ParameterizedTypeReference.class));
    }

    @Test
    void shouldRetryTemporaryHttpFailureAndSucceed() {
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
            .thenThrow(httpResponseException(503, "temporary unavailable"))
            .thenReturn(successResponse("ok"));

        String result = rpcHttpClient.call(request("getHealth"), STRING_RESPONSE);

        assertEquals("ok", result);
        verify(responseSpec, times(2)).body(any(ParameterizedTypeReference.class));
    }

    @Test
    void shouldThrowTimeoutAfterRetryExhausted() {
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
            .thenThrow(new ResourceAccessException("timeout", new SocketTimeoutException("read timed out")));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> rpcHttpClient.call(request("getHealth"), STRING_RESPONSE)
        );

        assertEquals(ErrorCode.BLOCKCHAIN_RPC_TIMEOUT, exception.getErrorCode());
        verify(responseSpec, times(3)).body(any(ParameterizedTypeReference.class));
    }

    @Test
    void shouldNotRetryNonRetryableRpcError() {
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
            .thenReturn(errorResponse(-32602, "bad request"));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> rpcHttpClient.call(request("getHealth"), STRING_RESPONSE)
        );

        assertEquals(ErrorCode.BLOCKCHAIN_RPC_ERROR, exception.getErrorCode());
        verify(responseSpec, times(1)).body(any(ParameterizedTypeReference.class));
    }

    private SolanaRpcRequestDto request(String method) {
        return SolanaRpcRequestDto.of(1, method, List.of());
    }

    private JsonRpcResponseDto<String> successResponse(String result) {
        JsonRpcResponseDto<String> response = new JsonRpcResponseDto<>();
        response.setJsonrpc("2.0");
        response.setId(1);
        response.setResult(result);
        return response;
    }

    private JsonRpcResponseDto<String> errorResponse(int code, String message) {
        JsonRpcResponseDto<String> response = new JsonRpcResponseDto<>();
        response.setJsonrpc("2.0");
        response.setId(1);
        com.stableflow.blockchain.dto.SolanaRpcErrorDto error = new com.stableflow.blockchain.dto.SolanaRpcErrorDto();
        error.setCode(code);
        error.setMessage(message);
        response.setError(error);
        return response;
    }

    private RestClientResponseException httpResponseException(int statusCode, String responseBody) {
        return new RestClientResponseException(
            "temporary http error",
            statusCode,
            "status",
            HttpHeaders.EMPTY,
            responseBody.getBytes(),
            null
        );
    }
}
