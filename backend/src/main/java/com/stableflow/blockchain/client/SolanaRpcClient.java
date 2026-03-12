package com.stableflow.blockchain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.blockchain.dto.SolanaRpcRequestDto;
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
import java.util.stream.StreamSupport;
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

        JsonNode result = call(
            SolanaRpcRequestDto.of(REQUEST_ID_SIGNATURES, "getSignaturesForAddress", List.of(address, options))
        );
        if (result == null || !result.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(result.spliterator(), false)
            .map(this::toSignatureVo)
            .toList();
    }

    @Override
    public SolanaTransactionDetailVo getTransaction(String signature) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("encoding", ENCODING_JSON_PARSED);
        options.put("commitment", COMMITMENT_CONFIRMED);
        options.put("maxSupportedTransactionVersion", 0);

        JsonNode result = call(
            SolanaRpcRequestDto.of(REQUEST_ID_TRANSACTION, "getTransaction", List.of(signature, options))
        );
        if (result == null || result.isNull()) {
            return null;
        }

        return toTransactionDetailVo(signature, result);
    }

    private JsonNode call(SolanaRpcRequestDto request) {
        JsonNode response = restClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(JsonNode.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Solana RPC returned empty response");
        }
        JsonNode error = response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Solana RPC error: " + error.path("code").asInt() + " " + error.path("message").asText("")
            );
        }
        return response.path("result");
    }

    private SolanaTransactionSignatureVo toSignatureVo(JsonNode node) {
        SolanaTransactionSignatureVo signatureVo = new SolanaTransactionSignatureVo();
        signatureVo.setSignature(node.path("signature").asText(null));
        signatureVo.setSlot(node.path("slot").isMissingNode() ? null : node.path("slot").asLong());
        signatureVo.setBlockTime(toOffsetDateTime(node.path("blockTime")));
        signatureVo.setError(
            node.path("err").isNull() || node.path("err").isMissingNode() ? null : node.path("err").toString()
        );
        return signatureVo;
    }

    private SolanaTransactionDetailVo toTransactionDetailVo(String signature, JsonNode result) {
        SolanaTransactionDetailVo detailVo = new SolanaTransactionDetailVo();
        detailVo.setSignature(signature);
        detailVo.setSlot(result.path("slot").isMissingNode() ? null : result.path("slot").asLong());
        detailVo.setBlockTime(toOffsetDateTime(result.path("blockTime")));
        detailVo.setMeta(toMetaVo(result.path("meta")));
        detailVo.setTransaction(toTransactionVo(result.path("transaction")));
        return detailVo;
    }

    private SolanaTransactionDetailVo.MetaVo toMetaVo(JsonNode metaNode) {
        if (metaNode == null || metaNode.isMissingNode() || metaNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.MetaVo metaVo = new SolanaTransactionDetailVo.MetaVo();
        metaVo.setSuccess(metaNode.path("err").isMissingNode() || metaNode.path("err").isNull());
        metaVo.setError(metaNode.path("err").isNull() || metaNode.path("err").isMissingNode() ? null : metaNode.path("err").toString());
        metaVo.setFee(metaNode.path("fee").isMissingNode() ? null : metaNode.path("fee").asLong());
        return metaVo;
    }

    private SolanaTransactionDetailVo.TransactionVo toTransactionVo(JsonNode transactionNode) {
        if (transactionNode == null || transactionNode.isMissingNode() || transactionNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.TransactionVo transactionVo = new SolanaTransactionDetailVo.TransactionVo();
        transactionVo.setMessage(toMessageVo(transactionNode.path("message")));
        return transactionVo;
    }

    private SolanaTransactionDetailVo.MessageVo toMessageVo(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.MessageVo messageVo = new SolanaTransactionDetailVo.MessageVo();
        messageVo.setAccountKeys(StreamSupport.stream(messageNode.path("accountKeys").spliterator(), false)
            .map(this::toAccountKeyVo)
            .toList());
        messageVo.setInstructions(StreamSupport.stream(messageNode.path("instructions").spliterator(), false)
            .map(this::toInstructionVo)
            .toList());
        return messageVo;
    }

    private SolanaTransactionDetailVo.AccountKeyVo toAccountKeyVo(JsonNode accountKeyNode) {
        SolanaTransactionDetailVo.AccountKeyVo accountKeyVo = new SolanaTransactionDetailVo.AccountKeyVo();
        if (accountKeyNode.isTextual()) {
            accountKeyVo.setPubkey(accountKeyNode.asText());
            return accountKeyVo;
        }
        accountKeyVo.setPubkey(accountKeyNode.path("pubkey").asText(null));
        accountKeyVo.setSigner(accountKeyNode.path("signer").isMissingNode() ? null : accountKeyNode.path("signer").asBoolean());
        accountKeyVo.setWritable(accountKeyNode.path("writable").isMissingNode() ? null : accountKeyNode.path("writable").asBoolean());
        accountKeyVo.setSource(accountKeyNode.path("source").asText(null));
        return accountKeyVo;
    }

    private SolanaTransactionDetailVo.InstructionVo toInstructionVo(JsonNode instructionNode) {
        SolanaTransactionDetailVo.InstructionVo instructionVo = new SolanaTransactionDetailVo.InstructionVo();
        instructionVo.setProgram(instructionNode.path("program").asText(null));
        instructionVo.setProgramId(instructionNode.path("programId").asText(null));
        instructionVo.setParsed(toParsedVo(instructionNode.path("parsed")));
        return instructionVo;
    }

    private SolanaTransactionDetailVo.ParsedVo toParsedVo(JsonNode parsedNode) {
        if (parsedNode == null || parsedNode.isMissingNode() || parsedNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.ParsedVo parsedVo = new SolanaTransactionDetailVo.ParsedVo();
        parsedVo.setType(parsedNode.path("type").asText(null));
        parsedVo.setInfo(toInfoVo(parsedNode.path("info")));
        return parsedVo;
    }

    private SolanaTransactionDetailVo.InfoVo toInfoVo(JsonNode infoNode) {
        if (infoNode == null || infoNode.isMissingNode() || infoNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.InfoVo infoVo = new SolanaTransactionDetailVo.InfoVo();
        infoVo.setSource(infoNode.path("source").asText(null));
        infoVo.setDestination(infoNode.path("destination").asText(null));
        infoVo.setAuthority(infoNode.path("authority").asText(null));
        infoVo.setMint(infoNode.path("mint").asText(null));
        infoVo.setOwner(infoNode.path("owner").asText(null));
        infoVo.setAccount(infoNode.path("account").asText(null));
        infoVo.setWallet(infoNode.path("wallet").asText(null));
        infoVo.setTokenAmount(toTokenAmountVo(infoNode.path("tokenAmount")));
        return infoVo;
    }

    private SolanaTransactionDetailVo.TokenAmountVo toTokenAmountVo(JsonNode tokenAmountNode) {
        if (tokenAmountNode == null || tokenAmountNode.isMissingNode() || tokenAmountNode.isNull()) {
            return null;
        }

        SolanaTransactionDetailVo.TokenAmountVo tokenAmountVo = new SolanaTransactionDetailVo.TokenAmountVo();
        tokenAmountVo.setAmount(tokenAmountNode.path("amount").asText(null));
        tokenAmountVo.setDecimals(tokenAmountNode.path("decimals").isMissingNode() ? null : tokenAmountNode.path("decimals").asInt());
        tokenAmountVo.setUiAmountString(tokenAmountNode.path("uiAmountString").asText(null));
        return tokenAmountVo;
    }

    private OffsetDateTime toOffsetDateTime(JsonNode epochSecondsNode) {
        if (epochSecondsNode == null || epochSecondsNode.isNull() || epochSecondsNode.isMissingNode()) {
            return null;
        }
        return Instant.ofEpochSecond(epochSecondsNode.asLong()).atOffset(ZoneOffset.UTC);
    }
}
