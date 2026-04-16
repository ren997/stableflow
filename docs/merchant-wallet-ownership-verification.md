# 商家固定地址所有权验证扩展说明

## 目标

`T103` 的目标不是在当前 MVP 就把所有链上钱包签名验签一次性做满，而是把“挑战码 -> 钱包签名 -> 验签状态 -> 后续替换真实验签器”的契约先收口稳定。

这样后续接入真实 Solana 钱包签名验证时，只需要替换验签实现，不需要再改数据库表结构、控制器接口或前端交互协议。

## 当前落地范围

当前代码已经补齐以下扩展位：

- `merchant_payment_config` 增加地址所有权验证相关字段
- 增加 `MerchantWalletOwnershipStatusEnum`
- 提供挑战码生成接口
- 提供签名提交接口
- 提供独立 `MerchantWalletOwnershipVerifierService` 验签扩展点
- 在收款地址或链变更时，自动重置旧验证状态，避免地址证明串用

## 状态模型

当前钱包地址所有权验证状态包括：

- `UNVERIFIED`
- `CHALLENGE_ISSUED`
- `SIGNATURE_SUBMITTED`
- `VERIFIED`
- `FAILED`

其中当前 MVP 默认会走到：

- 生成挑战后进入 `CHALLENGE_ISSUED`
- 提交签名后进入 `SIGNATURE_SUBMITTED`

说明：

- `SIGNATURE_SUBMITTED` 表示当前已经具备挑战码、挑战消息、钱包签名和提交流程
- 真实密码学验签能力未来接入后，可直接把该状态流转到 `VERIFIED` 或 `FAILED`

## 接口

当前新增接口：

- `POST /api/merchant/payment-config/ownership/challenge`
- `POST /api/merchant/payment-config/ownership/verify`

接口语义：

- `challenge`：为当前商家的固定收款地址生成一条带过期时间的挑战消息
- `verify`：提交对挑战消息的签名，并调用当前验签器实现

## 挑战消息契约

挑战消息当前包含以下信息：

- `merchantId`
- `walletAddress`
- `chain`
- `challengeCode`
- `issuedAt`
- `expiresAt`

这保证后续真实验签时，签名上下文和业务归属是明确的，不会只靠一个随机字符串做模糊校验。

## 当前验签实现

当前 `MerchantWalletOwnershipVerifierServiceImpl` 是占位实现：

- 会保留签名提交结果
- 会返回 `verifierReady = false`
- 不做真实的 Solana 钱包密码学验签

这是刻意设计，原因是当前 MVP 主线仍然是 `fixed address + reference` 的支付闭环，不应该让地址所有权验证扩展反向阻塞收款主链路。

## 后续接入建议

后续若要接入真实验签，建议按下面方式扩展：

1. 在 `MerchantWalletOwnershipVerifierServiceImpl` 中接入 Solana 钱包签名验签逻辑
2. 统一校验签名消息必须与 `ownership_challenge_message` 完全一致
3. 验签成功后写入 `VERIFIED` 与 `ownership_verified_at`
4. 验签失败时写入 `FAILED`
5. 如需支持多钱包协议，可在该服务层再按 `chain` 或签名方案继续分发

## 与 MVP 主链路关系

本扩展当前不参与支付真值判断。

支付是否成立，仍然必须以：

- 固定地址
- reference
- 交易详情解析
- 金额/币种/时窗验证
- 核销结果

作为唯一支付事实来源。
