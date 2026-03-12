package com.stableflow.blockchain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stableflow.blockchain.dto.GetTransactionResultDto;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SolanaTransactionConverterTest {

    private final SolanaTransactionConverter converter = new SolanaTransactionConverter();

    @Test
    void shouldConvertTransactionIntoBusinessFields() {
        GetTransactionResultDto transactionResultDto = buildTransferCheckedTransaction();

        SolanaTransactionDetailVo detailVo = converter.toTransactionDetailVo("tx-signature-1", transactionResultDto);

        assertNotNull(detailVo);
        assertEquals("tx-signature-1", detailVo.getSignature());
        assertTrue(detailVo.getSuccess());
        assertNull(detailVo.getError());
        assertEquals(5000L, detailVo.getFee());
        assertEquals("payer-wallet-1", detailVo.getPayerAddress());
        assertEquals("source-token-account-1", detailVo.getSourceAddress());
        assertEquals("merchant-token-account-1", detailVo.getRecipientAddress());
        assertEquals("usdc-mint-1", detailVo.getMintAddress());
        assertEquals(new BigDecimal("12.34"), detailVo.getAmount());
        assertEquals("transferChecked", detailVo.getTransferType());
        assertEquals(List.of("invoice-reference-1"), detailVo.getReferenceKeys());
        assertEquals("invoice-reference-1", detailVo.getPrimaryReferenceKey());
        assertNotNull(detailVo.getBlockTime());
    }

    @Test
    void shouldFallbackToRawAmountAndSignerForPayer() {
        GetTransactionResultDto transactionResultDto = buildTransferTransactionWithoutUiAmount();
        transactionResultDto.getMeta().setErr("InstructionError");

        SolanaTransactionDetailVo detailVo = converter.toTransactionDetailVo("tx-signature-2", transactionResultDto);

        assertNotNull(detailVo);
        assertFalse(detailVo.getSuccess());
        assertEquals("InstructionError", detailVo.getError());
        assertEquals("payer-wallet-2", detailVo.getPayerAddress());
        assertEquals(new BigDecimal("1.500000"), detailVo.getAmount());
        assertEquals(List.of("invoice-reference-2"), detailVo.getReferenceKeys());
    }

    private GetTransactionResultDto buildTransferCheckedTransaction() {
        GetTransactionResultDto resultDto = new GetTransactionResultDto();
        resultDto.setSlot(12345L);
        resultDto.setBlockTime(1_710_000_000L);

        GetTransactionResultDto.MetaDto metaDto = new GetTransactionResultDto.MetaDto();
        metaDto.setFee(5000L);
        resultDto.setMeta(metaDto);

        GetTransactionResultDto.InfoDto infoDto = new GetTransactionResultDto.InfoDto();
        infoDto.setSource("source-token-account-1");
        infoDto.setDestination("merchant-token-account-1");
        infoDto.setAuthority("payer-wallet-1");
        infoDto.setMint("usdc-mint-1");
        infoDto.setTokenAmount(tokenAmount("12340000", 6, "12.34"));

        resultDto.setTransaction(transaction(
            List.of(
                accountKey("payer-wallet-1", true, true),
                accountKey("source-token-account-1", false, true),
                accountKey("merchant-token-account-1", false, true),
                accountKey("usdc-mint-1", false, false),
                accountKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", false, false),
                accountKey("invoice-reference-1", false, false)
            ),
            List.of(instruction("spl-token", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", "transferChecked", infoDto))
        ));
        return resultDto;
    }

    private GetTransactionResultDto buildTransferTransactionWithoutUiAmount() {
        GetTransactionResultDto resultDto = new GetTransactionResultDto();
        resultDto.setSlot(67890L);
        resultDto.setBlockTime(1_710_000_100L);

        GetTransactionResultDto.MetaDto metaDto = new GetTransactionResultDto.MetaDto();
        metaDto.setFee(7000L);
        resultDto.setMeta(metaDto);

        GetTransactionResultDto.InfoDto infoDto = new GetTransactionResultDto.InfoDto();
        infoDto.setSource("source-token-account-2");
        infoDto.setDestination("merchant-token-account-2");
        infoDto.setMint("usdc-mint-2");
        infoDto.setTokenAmount(tokenAmount("1500000", 6, null));

        resultDto.setTransaction(transaction(
            List.of(
                accountKey("payer-wallet-2", true, true),
                accountKey("source-token-account-2", false, true),
                accountKey("merchant-token-account-2", false, true),
                accountKey("usdc-mint-2", false, false),
                accountKey("invoice-reference-2", false, false)
            ),
            List.of(instruction("spl-token", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA", "transfer", infoDto))
        ));
        return resultDto;
    }

    private GetTransactionResultDto.TransactionDto transaction(
        List<GetTransactionResultDto.AccountKeyDto> accountKeys,
        List<GetTransactionResultDto.InstructionDto> instructions
    ) {
        GetTransactionResultDto.MessageDto messageDto = new GetTransactionResultDto.MessageDto();
        messageDto.setAccountKeys(accountKeys);
        messageDto.setInstructions(instructions);

        GetTransactionResultDto.TransactionDto transactionDto = new GetTransactionResultDto.TransactionDto();
        transactionDto.setMessage(messageDto);
        return transactionDto;
    }

    private GetTransactionResultDto.AccountKeyDto accountKey(String pubkey, boolean signer, boolean writable) {
        GetTransactionResultDto.AccountKeyDto accountKeyDto = new GetTransactionResultDto.AccountKeyDto();
        accountKeyDto.setPubkey(pubkey);
        accountKeyDto.setSigner(signer);
        accountKeyDto.setWritable(writable);
        return accountKeyDto;
    }

    private GetTransactionResultDto.InstructionDto instruction(
        String program,
        String programId,
        String type,
        GetTransactionResultDto.InfoDto infoDto
    ) {
        GetTransactionResultDto.ParsedDto parsedDto = new GetTransactionResultDto.ParsedDto();
        parsedDto.setType(type);
        parsedDto.setInfo(infoDto);

        GetTransactionResultDto.InstructionDto instructionDto = new GetTransactionResultDto.InstructionDto();
        instructionDto.setProgram(program);
        instructionDto.setProgramId(programId);
        instructionDto.setParsed(parsedDto);
        return instructionDto;
    }

    private GetTransactionResultDto.TokenAmountDto tokenAmount(String amount, Integer decimals, String uiAmountString) {
        GetTransactionResultDto.TokenAmountDto tokenAmountDto = new GetTransactionResultDto.TokenAmountDto();
        tokenAmountDto.setAmount(amount);
        tokenAmountDto.setDecimals(decimals);
        tokenAmountDto.setUiAmountString(uiAmountString);
        return tokenAmountDto;
    }
}
