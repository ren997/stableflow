package com.stableflow.blockchain.vo;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SolanaTransactionDetailVo {

    /** Transaction signature / 交易签名 */
    private String signature;

    /** Slot number / 槽位号 */
    private Long slot;

    /** Block time in UTC / 区块时间（UTC） */
    private OffsetDateTime blockTime;

    /** Execution metadata / 执行元数据 */
    private MetaVo meta;

    /** Parsed transaction body / 结构化交易主体 */
    private TransactionVo transaction;

    @Data
    public static class MetaVo {

        /** Whether execution succeeded / 是否执行成功 */
        private Boolean success;

        /** Raw error payload if failed / 执行失败原始错误 */
        private String error;

        /** Network fee in lamports / 网络手续费 */
        private Long fee;
    }

    @Data
    public static class TransactionVo {

        /** Parsed message / 结构化消息体 */
        private MessageVo message;
    }

    @Data
    public static class MessageVo {

        /** Account keys involved in the transaction / 交易账户列表 */
        private List<AccountKeyVo> accountKeys;

        /** Top-level parsed instructions / 顶层解析指令 */
        private List<InstructionVo> instructions;
    }

    @Data
    public static class AccountKeyVo {

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
    public static class InstructionVo {

        /** Program name / 程序名称 */
        private String program;

        /** Program id / 程序 ID */
        private String programId;

        /** Parsed instruction payload / 解析后的指令载荷 */
        private ParsedVo parsed;
    }

    @Data
    public static class ParsedVo {

        /** Parsed instruction type / 解析后的指令类型 */
        private String type;

        /** Parsed instruction info / 解析后的指令信息 */
        private InfoVo info;
    }

    @Data
    public static class InfoVo {

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
        private TokenAmountVo tokenAmount;
    }

    @Data
    public static class TokenAmountVo {

        /** Raw amount / 原始金额 */
        private String amount;

        /** Decimal places / 小数位数 */
        private Integer decimals;

        /** UI amount string / 展示金额字符串 */
        private String uiAmountString;
    }
}
