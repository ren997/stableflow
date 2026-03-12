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
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class SolanaRpcClient implements SolanaClient {

    private static final String COMMITMENT_CONFIRMED = "confirmed";
    private static final String ENCODING_JSON_PARSED = "jsonParsed";
    private static final int REQUEST_ID_SIGNATURES = 1;
    private static final int REQUEST_ID_TRANSACTION = 2;

    private final RpcHttpClient rpcHttpClient;
    private final SolanaTransactionConverter solanaTransactionConverter;

    public SolanaRpcClient(
        RpcHttpClient rpcHttpClient,
        SolanaTransactionConverter solanaTransactionConverter
    ) {
        this.rpcHttpClient = rpcHttpClient;
        this.solanaTransactionConverter = solanaTransactionConverter;
    }

    @Override
    public List<SolanaTransactionSignatureVo> getSignaturesForAddress(String address, int limit) {
        validateAddress(address);
        validateLimit(limit);
        GetSignaturesForAddressOptionsDto optionsDto =
            new GetSignaturesForAddressOptionsDto(limit, COMMITMENT_CONFIRMED);

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
        return solanaTransactionConverter.toTransactionDetailVo(signature, result);
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
}
