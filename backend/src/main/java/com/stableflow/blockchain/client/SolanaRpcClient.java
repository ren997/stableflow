package com.stableflow.blockchain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.blockchain.vo.SolanaRpcResponseVo;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SolanaRpcClient implements SolanaClient {

    private static final String COMMITMENT_CONFIRMED = "confirmed";
    private static final String ENCODING_JSON_PARSED = "jsonParsed";
    private static final int REQUEST_ID_SIGNATURES = 1;
    private static final int REQUEST_ID_TRANSACTION = 2;

    private final RestClient restClient;

    public SolanaRpcClient(SolanaProperties solanaProperties) {
        this.restClient = RestClient.builder()
            .baseUrl(solanaProperties.rpcUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("limit", limit);
        options.put("commitment", COMMITMENT_CONFIRMED);

        SolanaRpcResponseVo response = call(
            SolanaRpcRequestDto.of(REQUEST_ID_SIGNATURES, "getSignaturesForAddress", List.of(address, options))
        );

        JsonNode result = response.result();
        if (result == null || !result.isArray()) {
            return List.of();
        }

        return result.stream()
            .map(node -> new SolanaTransactionSignatureVo(
                node.path("signature").asText(null),
                node.path("slot").isMissingNode() ? null : node.path("slot").asLong(),
                toOffsetDateTime(node.path("blockTime")),
                node.path("err").isNull() || node.path("err").isMissingNode() ? null : node.path("err").toString()
            ))
            .toList();
    }

    @Override
    public SolanaTransactionDetailVo getTransaction(String signature) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("encoding", ENCODING_JSON_PARSED);
        options.put("commitment", COMMITMENT_CONFIRMED);
        options.put("maxSupportedTransactionVersion", 0);

        SolanaRpcResponseVo response = call(
            SolanaRpcRequestDto.of(REQUEST_ID_TRANSACTION, "getTransaction", List.of(signature, options))
        );

        JsonNode result = response.result();
        if (result == null || result.isNull()) {
            return null;
        }

        return new SolanaTransactionDetailVo(
            signature,
            result.path("slot").isMissingNode() ? null : result.path("slot").asLong(),
            toOffsetDateTime(result.path("blockTime")),
            result
        );
    }

    private SolanaRpcResponseVo call(SolanaRpcRequestDto request) {
        SolanaRpcResponseVo response = restClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(SolanaRpcResponseVo.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Solana RPC returned empty response");
        }
        if (response.hasError()) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Solana RPC error: " + response.error().code() + " " + response.error().message()
            );
        }
        return response;
    }

    private OffsetDateTime toOffsetDateTime(JsonNode epochSecondsNode) {
        if (epochSecondsNode == null || epochSecondsNode.isNull() || epochSecondsNode.isMissingNode()) {
            return null;
        }
        return Instant.ofEpochSecond(epochSecondsNode.asLong()).atOffset(ZoneOffset.UTC);
    }
}
