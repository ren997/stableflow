package com.stableflow.blockchain.dto;

import java.util.List;
import lombok.Data;

@Data
public class GetTransactionResultDto {

    /** Slot number / 槽位号 */
    private Long slot;

    /** Block time in epoch seconds / 区块时间秒级时间戳 */
    private Long blockTime;

    /** Transaction metadata / 交易元数据 */
    private MetaDto meta;

    /** Parsed transaction / 结构化交易数据 */
    private TransactionDto transaction;

    @Data
    public static class MetaDto {

        /** Raw execution error payload / 原始执行错误信息 */
        private Object err;

        /** Network fee in lamports / 网络手续费 */
        private Long fee;
    }

    @Data
    public static class TransactionDto {

        /** Parsed message / 结构化消息体 */
        private MessageDto message;
    }

    @Data
    public static class MessageDto {

        /** Account keys list / 账户列表 */
        private List<AccountKeyDto> accountKeys;

        /** Parsed instructions / 结构化指令列表 */
        private List<InstructionDto> instructions;
    }

    @Data
    public static class AccountKeyDto {

        /** Account public key / 账户公钥 */
        private String pubkey;

        /** Whether signer / 是否签名账户 */
        private Boolean signer;

        /** Whether writable / 是否可写 */
        private Boolean writable;

        /** Source marker from RPC / RPC 来源标记 */
        private String source;
    }

    @Data
    public static class InstructionDto {

        /** Program name / 程序名称 */
        private String program;

        /** Program id / 程序 ID */
        private String programId;

        /** Parsed payload / 解析后的载荷 */
        private ParsedDto parsed;
    }

    @Data
    public static class ParsedDto {

        /** Parsed instruction type / 解析后的指令类型 */
        private String type;

        /** Parsed instruction info / 解析后的指令信息 */
        private InfoDto info;
    }

    @Data
    public static class InfoDto {

        /** Source wallet or token account / 来源账户 */
        private String source;

        /** Destination wallet or token account / 目标账户 */
        private String destination;

        /** Authority account / 授权账户 */
        private String authority;

        /** Token mint address / 代币 Mint 地址 */
        private String mint;

        /** Account owner / 账户拥有者 */
        private String owner;

        /** Account address / 账户地址 */
        private String account;

        /** Wallet address / 钱包地址 */
        private String wallet;

        /** Token amount details / 代币金额信息 */
        private TokenAmountDto tokenAmount;
    }

    @Data
    public static class TokenAmountDto {

        /** Raw amount / 原始金额 */
        private String amount;

        /** Decimal places / 小数位数 */
        private Integer decimals;

        /** UI amount string / 展示金额字符串 */
        private String uiAmountString;
    }
}
