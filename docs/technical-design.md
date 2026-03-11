# StableFlow 技术方案

## 1. 文档目标

本文档用于指导 StableFlow MVP 的后续开发与实现，重点回答以下问题：

- 系统采用什么技术栈
- 各模块如何拆分与协作
- Solana 支付接入、监听、验证、核销如何闭环
- 单体架构下如何处理异步任务、幂等、异常与可观测性
- 前后端如何选型，才能在黑客松周期内快速落地

本文档面向当前阶段的 MVP，默认约束如下：

- 后端采用 Java + Spring Boot
- 架构以模块化单体为主，不拆分分布式服务
- 缓存采用 Redis
- 异步处理优先使用应用内异步 + 数据库 outbox + 定时任务
- 若引入消息中间件，优先 RabbitMQ，不建议当前阶段使用 Kafka
- 前端以商家后台与公共支付页为主，不开发移动端 App

---

## 2. 架构目标与设计原则

### 2.1 架构目标

MVP 需要优先支撑以下能力：

- 商家配置固定 Solana USDC 收款地址
- 商家创建 Invoice
- 系统生成唯一 reference 与 Solana 原生支付请求
- 客户完成 Solana USDC 支付
- 系统监听链上交易、获取交易详情并完成验证
- 系统完成核销、异常处理、Payment Proof 和基础对账
- Agent 基于已验证的支付事实提供辅助操作

### 2.2 设计原则

- 业务优先：核心是 billing -> payment -> verification -> reconciliation 工作流
- 单体优先：当前阶段优先降低复杂度，先把闭环跑通
- 链上事实驱动：支付结果不能只依赖本地状态，必须基于链上交易验证
- reference 优先：订单归因主策略是固定收款地址 + reference
- 幂等优先：支付扫描、状态更新、Payment Proof 生成必须幂等
- 异步解耦：监听、验证、核销、对账彼此解耦
- 可演进：后续可以平滑演进到多链、多币种、Webhook 与开放 API

---

## 3. 技术选型

### 3.1 后端选型

推荐方案：

- JDK 21
- Spring Boot 3.x
- Spring Web
- Spring Validation
- Spring Security
- Spring Scheduling
- SpringDoc OpenAPI
- MyBatis-Plus
- PostgreSQL
- Redis

选择理由：

- JDK 21 是 LTS 版本，适合当前项目长期演进
- 你已有其他项目在使用 JDK 21，可降低上下文切换成本
- Spring Boot 3.x 对 JDK 21 支持成熟
- Java + Spring Boot 适合账单、状态机、定时任务、审计日志等强业务系统
- SpringDoc OpenAPI 可快速生成 Swagger UI，适合前后端联调、接口调试与 Demo 展示
- MyBatis-Plus 开发效率高，比 JPA 更适合当前明确的数据表与状态驱动场景
- PostgreSQL 在事务、JSON 字段、索引能力上更适合这类后台系统
- Redis 可同时承担缓存、幂等键、分布式锁、短期状态存储

### 3.2 虚拟线程使用建议

结论：

- 升级到 JDK 21
- 可以使用虚拟线程，但不把它作为系统成立的前提
- 当前阶段以“按需使用”而不是“全局替换”为原则

推荐使用场景：

- 调用 Solana RPC
- 调用邮件服务
- 调用 LLM API
- 处理 Webhook 分发
- 处理 I/O 密集型异步任务

### 3.3 异步与消息中间件建议

当前阶段明确建议：

- P0 不引入 Kafka
- P0 主路径采用：数据库状态表 + Spring Scheduler + Spring 异步执行器 + Outbox 事件表
- 若确实需要消息中间件，优先 RabbitMQ

推荐理由：

- 当前是模块化单体，不是多服务事件平台，Kafka 的运维和使用复杂度过高
- 当前异步任务主要是扫描、验证、对账，RabbitMQ 更适合工作队列、延迟重试和低成本接入
- Kafka 更适合高吞吐、流式分析、多消费者事件平台，不是当前 MVP 的核心痛点

### 3.4 数据库与缓存

数据库：

- PostgreSQL 16

缓存与状态：

- Redis 7

Redis 用途：

- 幂等键去重
- 热点查询缓存
- 支付扫描任务锁
- 短期支付状态缓存
- reference 去重与任务锁控制

### 3.5 前端选型建议

推荐方案：

- React
- Vite
- TypeScript
- Ant Design
- TanStack Query
- React Router

选择理由：

- 当前产品主要是中后台和支付页，不是内容站，没必要优先上 Next.js
- Vite + React 更简单，启动快、心智负担低，更适合黑客松周期
- Ant Design 很适合账单列表、筛选、表单、统计面板等中后台页面
- TanStack Query 适合处理后端 API 请求、缓存、刷新和状态同步

### 3.5 API 文档与调试建议

推荐方案：

- 使用 SpringDoc OpenAPI
- 启用 Swagger UI
- 支持 Bearer JWT 鉴权调试

选择理由：

- 可自动生成 OpenAPI 文档，减少手工维护接口文档成本
- 适合当前 MVP 阶段的接口联调、Demo 演示与快速验收
- 可直接在 Swagger UI 中调试受保护接口，提升开发效率

### 3.6 Solana 接入建议

推荐方案：

- 使用托管 RPC 服务，例如 Helius 或 QuickNode
- 后端封装独立 `SolanaClient`
- 优先采用轮询扫描 + 交易详情查询的方式完成 MVP
- 支付请求采用 Solana 原生支付链接 / 二维码
- 订单归因优先使用 `recipient + reference`

原因：

- 托管 RPC 更稳定，适合黑客松 demo 和后续线上演示
- 轮询方案更容易控制、排查和重试
- 固定地址 + reference 更适合 Solana Native 支付体验
- 相比一单一地址，能显著降低收款地址和私钥管理复杂度

---

## 4. 总体架构

### 4.1 架构形态

当前采用模块化单体架构：

- 单个 Spring Boot 应用
- 单个 PostgreSQL
- 单个 Redis
- 前端与后端分离部署
- 可选 RabbitMQ，非 P0 必需

### 4.2 模块划分

推荐按包或模块拆分：

- `auth`：登录、商家身份认证、权限校验
- `merchant`：商家信息、固定收款地址配置
- `invoice`：账单创建、编辑、状态管理
- `payment`：支付请求、支付状态查询、支付页信息展示
- `blockchain`：Solana RPC 封装、交易扫描、交易解析
- `verification`：支付验证、订单归因、状态判定
- `reconciliation`：核销处理、Payment Proof、异常处理
- `dashboard`：统计查询、对账汇总、异常筛选
- `agent`：自然语言开单、异常解释、对账总结
- `common`：异常、工具类、基础枚举、审计字段、响应模型

说明：

- `notification` 不作为 P0 主模块，可在后续阶段引入
- P0 优先完成 invoice、payment、blockchain、verification、reconciliation 五个核心模块

### 4.3 逻辑分层

推荐分层如下：

- Controller：对外提供 REST API
- Application Service：编排业务流程
- Domain Service：封装核心业务规则与状态流转
- Infrastructure：数据库、Redis、Solana RPC
- Scheduler / Worker：异步扫描、验证、对账任务

---

## 5. 核心业务链路设计

### 5.1 主链路

主链路如下：

1. 商家配置固定收款地址
2. 商家创建 Invoice
3. 系统为该 Invoice 生成唯一 reference、支付链接和支付二维码
4. 客户使用 Solana 钱包支付 USDC
5. 后端按商家固定收款地址扫描候选交易，并结合 reference 定位目标交易
6. 后端获取交易详情并解析 token transfer
7. 后端验证收款地址、reference、币种、金额、有效期、幂等性
8. 后端更新 Invoice 状态并写入核销记录
9. 后端生成 Payment Proof
10. 后端更新 Dashboard 聚合结果
11. Agent 基于已验证结果提供解释或总结

### 5.2 为什么区分监听、验证、核销

这三步必须明确拆开：

- 监听：发现候选交易
- 验证：判断该交易是否满足该 Invoice 的支付条件
- 核销：基于验证结果更新业务状态并沉淀结果

这样做的原因：

- 避免“地址收到了钱就直接判成功”
- 支持少付、多付、过期到账、错误币种、reference 缺失等异常分支
- 便于做幂等、重试、异常回放
- 更符合 Solana 黑客松对链上验证闭环的期待

---

## 6. 支付接入与链上验证设计

### 6.1 支付请求设计

每张 Invoice 在创建后生成一份支付请求，支付请求包含：

- 商家固定收款地址
- 账单金额
- USDC mint
- unique reference
- label
- message
- payment link

说明：

- 商家固定收款地址是资产入口
- reference 是订单归因主锚点
- 支付页同时展示支付链接、二维码和支付摘要

### 6.2 支付监听方案

MVP 推荐方案：

- 使用定时任务按批扫描商家固定收款地址相关交易
- 通过 RPC 查询候选交易签名列表
- 对新增签名拉取交易详情
- 解析转账指令中的收款地址、mint、amount、reference
- 将交易写入 `payment_transaction` 并进入验证队列

### 6.3 链上验证规则

对每笔候选交易，需要完成以下验证：

- 交易状态是否成功
- 接收地址是否为该商家的固定收款地址
- 交易 reference 是否匹配该 Invoice
- 转账资产是否为指定 USDC mint
- 金额是否等于、小于或大于应付金额
- blockTime 是否在 Invoice 有效期内
- 该交易是否已经处理过
- 是否已存在更早的有效支付结果

验证输出：

- `PAID`
- `PARTIALLY_PAID`
- `OVERPAID`
- `EXPIRED + LATE_PAYMENT`
- `FAILED_RECONCILIATION`
- `UNMATCHED_PAYMENT`
- `WRONG_CURRENCY`
- `MISSING_REFERENCE`
- `INVALID_REFERENCE`

### 6.4 Payment Proof 设计

MVP 不上链上智能合约，但需要提供“可验证支付结果”。

Payment Proof 至少包含：

- invoiceId
- txHash
- payerAddress
- recipientAddress
- reference
- mintAddress
- amount
- paidAt
- verificationResult
- finalStatus
- exceptionTags
- explanation

作用：

- 商家查看支付详情
- Dashboard 展示支付证据
- Agent 基于已验证事实生成说明
- 后续可扩展为商家 API 或 Webhook 输出

---

## 7. 状态机设计

### 7.1 Invoice 主状态

- `DRAFT`
- `PENDING`
- `PAID`
- `PARTIALLY_PAID`
- `OVERPAID`
- `EXPIRED`
- `FAILED_RECONCILIATION`

### 7.2 异常标签

- `LATE_PAYMENT`
- `WRONG_CURRENCY`
- `DUPLICATE_PAYMENT`
- `UNMATCHED_PAYMENT`
- `MISSING_REFERENCE`
- `INVALID_REFERENCE`
- `PAYMENT_DELAYED`

### 7.3 状态流转规则

- 创建后：`DRAFT -> PENDING`
- 足额支付：`PENDING -> PAID`
- 少付：`PENDING -> PARTIALLY_PAID`
- 多付：`PENDING -> OVERPAID`
- 到期未支付：`PENDING -> EXPIRED`
- 验证失败或核销失败：`* -> FAILED_RECONCILIATION`

说明：

- 异常标签和主状态并存
- 过期到账推荐记录为 `EXPIRED + LATE_PAYMENT`
- 重复支付不直接覆盖已完成状态，而是追加异常标签和新交易记录

---

## 8. 数据模型设计

### 8.1 merchant

核心字段：

- `id`
- `merchant_name`
- `email`
- `password_hash`
- `status`
- `created_at`
- `updated_at`

### 8.2 merchant_payment_config

核心字段：

- `id`
- `merchant_id`
- `wallet_address`
- `mint_address`
- `chain`
- `active_flag`
- `created_at`
- `updated_at`

### 8.3 invoice

核心字段：

- `id`
- `public_id`
- `merchant_id`
- `invoice_no`
- `customer_name`
- `amount`
- `currency`
- `chain`
- `description`
- `status`
- `exception_tags`
- `expire_at`
- `paid_at`
- `created_at`
- `updated_at`

### 8.4 invoice_payment_request

核心字段：

- `id`
- `invoice_id`
- `recipient_address`
- `reference_key`
- `mint_address`
- `expected_amount`
- `payment_link`
- `label`
- `message`
- `expire_at`
- `created_at`

### 8.5 payment_transaction

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `reference_key`
- `payer_address`
- `recipient_address`
- `amount`
- `currency`
- `mint_address`
- `block_time`
- `verification_result`
- `payment_status`
- `raw_payload`
- `created_at`

### 8.6 reconciliation_record

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `reconciliation_status`
- `result_message`
- `exception_tags`
- `processed_at`
- `created_at`

### 8.7 payment_proof

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `proof_type`
- `proof_payload`
- `created_at`

### 8.8 outbox_event

核心字段：

- `id`
- `event_type`
- `aggregate_type`
- `aggregate_id`
- `payload`
- `status`
- `retry_count`
- `last_error`
- `next_retry_at`
- `created_at`
- `updated_at`

---

## 9. API 设计

### 9.1 认证接口

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### 9.2 商家配置接口

- `POST /api/merchant/payment-config`
- `GET /api/merchant/payment-config`

### 9.3 Invoice 接口

- `POST /api/invoices`
- `PUT /api/invoices/{id}`
- `GET /api/invoices/{id}`
- `GET /api/invoices`
- `POST /api/invoices/{id}/activate`
- `POST /api/invoices/{id}/reconcile`

### 9.4 支付接口

- `GET /api/invoices/{id}/payment-info`
- `GET /api/invoices/{id}/payment-status`
- `GET /api/invoices/{id}/payment-proof`

### 9.5 Dashboard 接口

- `GET /api/dashboard/summary`
- `GET /api/dashboard/invoices/status`
- `GET /api/dashboard/invoices/exceptions`

### 9.6 Agent 接口

- `POST /api/agent/create-invoice`
- `POST /api/agent/exception-explain`
- `POST /api/agent/reconciliation-summary`

---

## 10. 异步任务设计

### 10.1 必备定时任务

- `InvoiceExpireJob`：扫描到期账单并更新状态
- `PaymentScanJob`：扫描固定收款地址相关交易
- `PaymentVerifyJob`：处理待验证交易
- `OutboxDispatchJob`：分发 outbox 事件

### 10.2 推荐执行方式

第一阶段：

- Spring Scheduler 扫描数据库任务表 / outbox_event
- 每类任务单独线程池
- Redis 锁避免多实例重复执行
- 对 I/O 密集任务可评估使用虚拟线程执行器

第二阶段：

- 需要更好的削峰、重试和异步隔离时，引入 RabbitMQ

---

## 11. 安全与权限设计

### 11.1 认证方案

推荐：

- Spring Security + JWT
- 密码哈希采用 BCrypt

### 11.2 权限边界

- 商家只能查看自己的 Invoice、交易、Payment Proof 和统计
- 公共支付页使用 `public_id` 而不是数据库自增 id
- Agent 接口默认只能访问当前商家的数据

### 11.3 配置安全

需要保护的配置：

- 数据库连接串
- Redis 密码
- RPC API Key
- LLM API Key
- JWT Secret

建议：

- 使用环境变量管理
- 严禁提交到 Git 仓库

---

## 12. 可观测性与运维

### 12.1 日志

必须记录以下日志：

- Invoice 创建日志
- 支付扫描日志
- 交易验证日志
- 核销日志
- 异常单日志
- Agent 调用日志

建议日志字段：

- `traceId`
- `merchantId`
- `invoiceId`
- `reference`
- `txHash`
- `status`

### 12.2 监控指标

建议统计：

- 待支付账单数
- 已支付账单数
- 异常账单数
- 支付扫描耗时
- 支付验证成功率
- Payment Proof 生成成功率

---

## 13. 测试策略

### 13.1 单元测试

重点覆盖：

- Invoice 状态流转
- 支付验证规则
- reference 归因规则
- 异常标签判定
- 幂等处理逻辑

### 13.2 集成测试

重点覆盖：

- 创建账单 -> 生成支付信息
- 扫描交易 -> 验证 -> 核销
- 少付、多付、过期到账
- 缺少 reference / reference 不匹配
- 重复扫描同一 txHash

---

## 14. 开发优先级建议

### 14.1 P0

- 商家基础认证
- 商家固定收款地址配置
- Invoice 创建、列表、详情
- 支付链接 / 二维码生成
- PaymentScanJob
- 交易详情解析
- reference 归因与支付验证
- Reconciliation + Payment Proof
- Dashboard 基础汇总

### 14.2 P1

- 异常账单页
- Agent 自然语言开单
- Agent 异常解释
- Agent 对账总结

### 14.3 P2

- RabbitMQ 接入
- Webhook
- 多钱包更深兼容
- 更复杂权限体系
- 自动归集
- 通知体系增强

---

## 15. 最终建议

当前项目最适合的落地方式是：

- 架构采用模块化单体
- 后端采用 JDK 21 + Spring Boot + MyBatis-Plus + PostgreSQL + Redis
- 前端采用 React + Vite + TypeScript + Ant Design
- 异步优先使用 Scheduler + Outbox，不急着上 Kafka
- 如异步复杂度提高，再引入 RabbitMQ
- 产品主线坚持 billing -> payment -> verification -> reconciliation
- 支付策略采用“固定地址 + reference”
- Agent 作为增强层，不与支付判定主链路耦合

一句话总结：

**先做一个稳定、清晰、可演示的 Solana Stablecoin Billing & Reconciliation MVP，再逐步演进为更完整的商家收款基础设施。**
