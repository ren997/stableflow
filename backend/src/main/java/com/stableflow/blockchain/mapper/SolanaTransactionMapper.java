package com.stableflow.blockchain.mapper;

import com.stableflow.blockchain.dto.GetSignaturesForAddressResultDto;
import com.stableflow.blockchain.dto.GetTransactionResultDto;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SolanaTransactionMapper {

    public List<SolanaTransactionSignatureVo> toSignatureVos(List<GetSignaturesForAddressResultDto> resultDtos) {
        if (resultDtos == null || resultDtos.isEmpty()) {
            return List.of();
        }
        return resultDtos.stream().map(this::toSignatureVo).toList();
    }

    public SolanaTransactionDetailVo toTransactionDetailVo(String signature, GetTransactionResultDto resultDto) {
        if (resultDto == null) {
            return null;
        }

        SolanaTransactionDetailVo detailVo = new SolanaTransactionDetailVo();
        detailVo.setSignature(signature);
        detailVo.setSlot(resultDto.getSlot());
        detailVo.setBlockTime(toOffsetDateTime(resultDto.getBlockTime()));
        detailVo.setMeta(toMetaVo(resultDto.getMeta()));
        detailVo.setTransaction(toTransactionVo(resultDto.getTransaction()));
        return detailVo;
    }

    private SolanaTransactionSignatureVo toSignatureVo(GetSignaturesForAddressResultDto resultDto) {
        SolanaTransactionSignatureVo signatureVo = new SolanaTransactionSignatureVo();
        signatureVo.setSignature(resultDto.getSignature());
        signatureVo.setSlot(resultDto.getSlot());
        signatureVo.setBlockTime(toOffsetDateTime(resultDto.getBlockTime()));
        signatureVo.setError(resultDto.getErr() == null ? null : resultDto.getErr().toString());
        return signatureVo;
    }

    private SolanaTransactionDetailVo.MetaVo toMetaVo(GetTransactionResultDto.MetaDto metaDto) {
        if (metaDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.MetaVo metaVo = new SolanaTransactionDetailVo.MetaVo();
        metaVo.setSuccess(metaDto.getErr() == null);
        metaVo.setError(metaDto.getErr() == null ? null : metaDto.getErr().toString());
        metaVo.setFee(metaDto.getFee());
        return metaVo;
    }

    private SolanaTransactionDetailVo.TransactionVo toTransactionVo(GetTransactionResultDto.TransactionDto transactionDto) {
        if (transactionDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.TransactionVo transactionVo = new SolanaTransactionDetailVo.TransactionVo();
        transactionVo.setMessage(toMessageVo(transactionDto.getMessage()));
        return transactionVo;
    }

    private SolanaTransactionDetailVo.MessageVo toMessageVo(GetTransactionResultDto.MessageDto messageDto) {
        if (messageDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.MessageVo messageVo = new SolanaTransactionDetailVo.MessageVo();
        messageVo.setAccountKeys(messageDto.getAccountKeys() == null
            ? List.of()
            : messageDto.getAccountKeys().stream().map(this::toAccountKeyVo).toList());
        messageVo.setInstructions(messageDto.getInstructions() == null
            ? List.of()
            : messageDto.getInstructions().stream().map(this::toInstructionVo).toList());
        return messageVo;
    }

    private SolanaTransactionDetailVo.AccountKeyVo toAccountKeyVo(GetTransactionResultDto.AccountKeyDto accountKeyDto) {
        SolanaTransactionDetailVo.AccountKeyVo accountKeyVo = new SolanaTransactionDetailVo.AccountKeyVo();
        if (accountKeyDto == null) {
            return accountKeyVo;
        }
        accountKeyVo.setPubkey(accountKeyDto.getPubkey());
        accountKeyVo.setSigner(accountKeyDto.getSigner());
        accountKeyVo.setWritable(accountKeyDto.getWritable());
        accountKeyVo.setSource(accountKeyDto.getSource());
        return accountKeyVo;
    }

    private SolanaTransactionDetailVo.InstructionVo toInstructionVo(GetTransactionResultDto.InstructionDto instructionDto) {
        SolanaTransactionDetailVo.InstructionVo instructionVo = new SolanaTransactionDetailVo.InstructionVo();
        if (instructionDto == null) {
            return instructionVo;
        }
        instructionVo.setProgram(instructionDto.getProgram());
        instructionVo.setProgramId(instructionDto.getProgramId());
        instructionVo.setParsed(toParsedVo(instructionDto.getParsed()));
        return instructionVo;
    }

    private SolanaTransactionDetailVo.ParsedVo toParsedVo(GetTransactionResultDto.ParsedDto parsedDto) {
        if (parsedDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.ParsedVo parsedVo = new SolanaTransactionDetailVo.ParsedVo();
        parsedVo.setType(parsedDto.getType());
        parsedVo.setInfo(toInfoVo(parsedDto.getInfo()));
        return parsedVo;
    }

    private SolanaTransactionDetailVo.InfoVo toInfoVo(GetTransactionResultDto.InfoDto infoDto) {
        if (infoDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.InfoVo infoVo = new SolanaTransactionDetailVo.InfoVo();
        infoVo.setSource(infoDto.getSource());
        infoVo.setDestination(infoDto.getDestination());
        infoVo.setAuthority(infoDto.getAuthority());
        infoVo.setMint(infoDto.getMint());
        infoVo.setOwner(infoDto.getOwner());
        infoVo.setAccount(infoDto.getAccount());
        infoVo.setWallet(infoDto.getWallet());
        infoVo.setTokenAmount(toTokenAmountVo(infoDto.getTokenAmount()));
        return infoVo;
    }

    private SolanaTransactionDetailVo.TokenAmountVo toTokenAmountVo(GetTransactionResultDto.TokenAmountDto tokenAmountDto) {
        if (tokenAmountDto == null) {
            return null;
        }

        SolanaTransactionDetailVo.TokenAmountVo tokenAmountVo = new SolanaTransactionDetailVo.TokenAmountVo();
        tokenAmountVo.setAmount(tokenAmountDto.getAmount());
        tokenAmountVo.setDecimals(tokenAmountDto.getDecimals());
        tokenAmountVo.setUiAmountString(tokenAmountDto.getUiAmountString());
        return tokenAmountVo;
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC);
    }
}
