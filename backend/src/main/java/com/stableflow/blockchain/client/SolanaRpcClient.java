package com.stableflow.blockchain.client;

import com.stableflow.blockchain.converter.SolanaTransactionConverter;
import com.stableflow.blockchain.dto.GetSignaturesForAddressOptionsDto;
import com.stableflow.blockchain.dto.GetSignaturesForAddressResultDto;
import com.stableflow.blockchain.dto.GetTransactionResultDto;
import com.stableflow.blockchain.dto.GetTransactionOptionsDto;
import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SolanaRpcClient implements SolanaClient {

    private static final String COMMITMENT_CONFIRMED = "confirmed";
    private static final String ENCODING_JSON_PARSED = "jsonParsed";
    private static final int REQUEST_ID_SIGNATURES = 1;
    private static final int REQUEST_ID_TRANSACTION = 2;

    private final RpcHttpClient rpcHttpClient;
    private final SolanaTransactionConverter solanaTransactionConverter;
    private final ObjectMapper objectMapper;

    @Override
    public List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit) {
        return getSignaturesForAddress(address, limit, null);
    }

    @Override
    public List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit, String beforeSignature) {
        validateAddress(address);
        validateLimit(limit);
        GetSignaturesForAddressOptionsDto optionsDto =
            new GetSignaturesForAddressOptionsDto(limit, beforeSignature, COMMITMENT_CONFIRMED);

        // Build request -> execute shared RPC HTTP call -> map RPC DTO into service VO.
        List<GetSignaturesForAddressResultDto> result = rpcHttpClient.call(
            SolanaRpcRequestDto.of(REQUEST_ID_SIGNATURES, "getSignaturesForAddress", List.of(address, optionsDto)),
            new ParameterizedTypeReference<JsonRpcResponseDto<List<GetSignaturesForAddressResultDto>>>() {}
        );
        return solanaTransactionConverter.toSignatureVos(result);
    }

    @Override
    public SolanaTransactionDetailVo getTransaction(String signature) {
        validateSignature(signature);
        GetTransactionOptionsDto optionsDto =
            new GetTransactionOptionsDto(ENCODING_JSON_PARSED, COMMITMENT_CONFIRMED, 0);

        GetTransactionResultDto result = rpcHttpClient.call(
            SolanaRpcRequestDto.of(REQUEST_ID_TRANSACTION, "getTransaction", List.of(signature, optionsDto)),
            new ParameterizedTypeReference<JsonRpcResponseDto<GetTransactionResultDto>>() {}
        );
        if (result == null) {
            return null;
        }
        SolanaTransactionDetailVo detailVo = solanaTransactionConverter.toTransactionDetailVo(signature, result);
        detailVo.setRawPayload(serializeRawPayload(result));
        return detailVo;
    }

    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "address must not be blank");
        }
    }

    private void validateSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "signature must not be blank");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "limit must be greater than 0");
        }
    }

    private String serializeRawPayload(GetTransactionResultDto result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Failed to serialize Solana transaction payload: " + ex.getOriginalMessage()
            );
        }
    }
}
