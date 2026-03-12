package com.stableflow.blockchain.converter;

import com.stableflow.blockchain.dto.GetSignaturesForAddressResultDto;
import com.stableflow.blockchain.dto.GetTransactionResultDto;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SolanaTransactionConverter {

    private static final Set<String> TOKEN_TRANSFER_TYPES = Set.of("transfer", "transferchecked");
    private static final Set<String> KNOWN_PROGRAM_IDS = Set.of(
        "11111111111111111111111111111111",
        "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
        "ComputeBudget111111111111111111111111111111",
        "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr",
        "SysvarRent111111111111111111111111111111111",
        "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    );

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
        detailVo.setSuccess(resultDto.getMeta() == null || resultDto.getMeta().getErr() == null);
        detailVo.setError(resultDto.getMeta() == null || resultDto.getMeta().getErr() == null
            ? null
            : resultDto.getMeta().getErr().toString());
        detailVo.setFee(resultDto.getMeta() == null ? null : resultDto.getMeta().getFee());

        TransferSnapshot transferSnapshot = extractPrimaryTransfer(resultDto);
        detailVo.setPayerAddress(resolvePayerAddress(resultDto, transferSnapshot));
        detailVo.setSourceAddress(transferSnapshot == null ? null : transferSnapshot.sourceAddress());
        detailVo.setRecipientAddress(transferSnapshot == null ? null : transferSnapshot.recipientAddress());
        detailVo.setMintAddress(transferSnapshot == null ? null : transferSnapshot.mintAddress());
        detailVo.setAmount(transferSnapshot == null ? null : transferSnapshot.amount());
        detailVo.setTransferType(transferSnapshot == null ? null : transferSnapshot.transferType());

        List<String> referenceKeys = extractReferenceKeys(resultDto, transferSnapshot);
        detailVo.setReferenceKeys(referenceKeys);
        detailVo.setPrimaryReferenceKey(referenceKeys.isEmpty() ? null : referenceKeys.getFirst());
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

    private TransferSnapshot extractPrimaryTransfer(GetTransactionResultDto resultDto) {
        if (resultDto == null || resultDto.getTransaction() == null || resultDto.getTransaction().getMessage() == null) {
            return null;
        }

        List<GetTransactionResultDto.InstructionDto> instructions = resultDto.getTransaction().getMessage().getInstructions();
        if (instructions == null || instructions.isEmpty()) {
            return null;
        }

        for (GetTransactionResultDto.InstructionDto instructionDto : instructions) {
            if (instructionDto == null || instructionDto.getParsed() == null || instructionDto.getParsed().getInfo() == null) {
                continue;
            }

            String transferType = instructionDto.getParsed().getType();
            if (transferType == null || !TOKEN_TRANSFER_TYPES.contains(transferType.toLowerCase(Locale.ROOT))) {
                continue;
            }

            GetTransactionResultDto.InfoDto infoDto = instructionDto.getParsed().getInfo();
            BigDecimal amount = parseAmount(infoDto.getTokenAmount());
            if (amount == null) {
                continue;
            }

            return new TransferSnapshot(
                transferType,
                infoDto.getSource(),
                infoDto.getDestination(),
                infoDto.getAuthority(),
                infoDto.getMint(),
                amount,
                instructionDto.getProgramId()
            );
        }
        return null;
    }

    private String resolvePayerAddress(GetTransactionResultDto resultDto, TransferSnapshot transferSnapshot) {
        if (transferSnapshot != null && hasText(transferSnapshot.authorityAddress())) {
            return transferSnapshot.authorityAddress();
        }
        if (resultDto == null || resultDto.getTransaction() == null || resultDto.getTransaction().getMessage() == null) {
            return null;
        }

        List<GetTransactionResultDto.AccountKeyDto> accountKeys = resultDto.getTransaction().getMessage().getAccountKeys();
        if (accountKeys == null || accountKeys.isEmpty()) {
            return null;
        }

        return accountKeys.stream()
            .filter(accountKeyDto -> accountKeyDto != null && Boolean.TRUE.equals(accountKeyDto.getSigner()))
            .map(GetTransactionResultDto.AccountKeyDto::getPubkey)
            .filter(this::hasText)
            .findFirst()
            .orElse(null);
    }

    private List<String> extractReferenceKeys(GetTransactionResultDto resultDto, TransferSnapshot transferSnapshot) {
        if (resultDto == null || resultDto.getTransaction() == null || resultDto.getTransaction().getMessage() == null) {
            return List.of();
        }

        List<GetTransactionResultDto.AccountKeyDto> accountKeys = resultDto.getTransaction().getMessage().getAccountKeys();
        if (accountKeys == null || accountKeys.isEmpty()) {
            return List.of();
        }

        Set<String> excludedPubkeys = new LinkedHashSet<>(KNOWN_PROGRAM_IDS);
        collectInstructionPubkeys(resultDto, excludedPubkeys);
        if (transferSnapshot != null) {
            addIfPresent(excludedPubkeys, transferSnapshot.authorityAddress());
            addIfPresent(excludedPubkeys, transferSnapshot.sourceAddress());
            addIfPresent(excludedPubkeys, transferSnapshot.recipientAddress());
            addIfPresent(excludedPubkeys, transferSnapshot.mintAddress());
            addIfPresent(excludedPubkeys, transferSnapshot.programId());
        }

        return accountKeys.stream()
            .filter(accountKeyDto -> accountKeyDto != null && hasText(accountKeyDto.getPubkey()))
            .filter(accountKeyDto -> !Boolean.TRUE.equals(accountKeyDto.getSigner()))
            .filter(accountKeyDto -> !Boolean.TRUE.equals(accountKeyDto.getWritable()))
            .map(GetTransactionResultDto.AccountKeyDto::getPubkey)
            .filter(pubkey -> !excludedPubkeys.contains(pubkey))
            .distinct()
            .toList();
    }

    private void collectInstructionPubkeys(GetTransactionResultDto resultDto, Set<String> excludedPubkeys) {
        List<GetTransactionResultDto.InstructionDto> instructions = resultDto.getTransaction().getMessage().getInstructions();
        if (instructions == null || instructions.isEmpty()) {
            return;
        }

        for (GetTransactionResultDto.InstructionDto instructionDto : instructions) {
            if (instructionDto == null) {
                continue;
            }
            addIfPresent(excludedPubkeys, instructionDto.getProgramId());
            if (instructionDto.getParsed() == null || instructionDto.getParsed().getInfo() == null) {
                continue;
            }
            GetTransactionResultDto.InfoDto infoDto = instructionDto.getParsed().getInfo();
            addIfPresent(excludedPubkeys, infoDto.getSource());
            addIfPresent(excludedPubkeys, infoDto.getDestination());
            addIfPresent(excludedPubkeys, infoDto.getAuthority());
            addIfPresent(excludedPubkeys, infoDto.getMint());
            addIfPresent(excludedPubkeys, infoDto.getOwner());
            addIfPresent(excludedPubkeys, infoDto.getAccount());
            addIfPresent(excludedPubkeys, infoDto.getWallet());
        }
    }

    private BigDecimal parseAmount(GetTransactionResultDto.TokenAmountDto tokenAmountDto) {
        if (tokenAmountDto == null) {
            return null;
        }
        if (hasText(tokenAmountDto.getUiAmountString())) {
            return new BigDecimal(tokenAmountDto.getUiAmountString());
        }
        if (!hasText(tokenAmountDto.getAmount()) || tokenAmountDto.getDecimals() == null) {
            return null;
        }
        return new BigDecimal(tokenAmountDto.getAmount()).movePointLeft(tokenAmountDto.getDecimals());
    }

    private void addIfPresent(Set<String> values, String candidate) {
        if (hasText(candidate)) {
            values.add(candidate);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC);
    }

    private record TransferSnapshot(
        String transferType,
        String sourceAddress,
        String recipientAddress,
        String authorityAddress,
        String mintAddress,
        BigDecimal amount,
        String programId
    ) {
    }
}
