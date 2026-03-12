package com.stableflow.blockchain.client;

import com.stableflow.blockchain.converter.SolanaTransactionConverter;
import com.stableflow.blockchain.dto.GetSignaturesForAddressResultDto;
import com.stableflow.blockchain.dto.GetTransactionResultDto;
import com.stableflow.blockchain.dto.JsonRpcResponseDto;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("limit", limit);
        options.put("commitment", COMMITMENT_CONFIRMED);

        // Build request -> execute shared RPC HTTP call -> map RPC DTO into service VO.
        List<GetSignaturesForAddressResultDto> result = rpcHttpClient.call(
            SolanaRpcRequestDto.of(REQUEST_ID_SIGNATURES, "getSignaturesForAddress", List.of(address, options)),
            new ParameterizedTypeReference<JsonRpcResponseDto<List<GetSignaturesForAddressResultDto>>>() {}
        );
        return solanaTransactionConverter.toSignatureVos(result);
    }

    @Override
    public SolanaTransactionDetailVo getTransaction(String signature) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("encoding", ENCODING_JSON_PARSED);
        options.put("commitment", COMMITMENT_CONFIRMED);
        options.put("maxSupportedTransactionVersion", 0);

        GetTransactionResultDto result = rpcHttpClient.call(
            SolanaRpcRequestDto.of(REQUEST_ID_TRANSACTION, "getTransaction", List.of(signature, options)),
            new ParameterizedTypeReference<JsonRpcResponseDto<GetTransactionResultDto>>() {}
        );
        if (result == null) {
            return null;
        }
        return solanaTransactionConverter.toTransactionDetailVo(signature, result);
    }
}
