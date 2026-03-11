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

- 商家创建 Invoice
- 系统生成唯一收款地址与 Solana 原生支付请求
- 客户完成 Solana USDC 支付
- 系统监听链上交易、获取交易详情并完成验证
- 系统完成核销、异常处理、通知和基础对账
- Agent 基于已验证的支付事实提供辅助操作

### 2.2 设计原则

- 业务优先：核心是 billing -> payment -> verification -> reconciliation 工作流
- 单体优先：当前阶段优先降低复杂度，先把闭环跑通
- 链上事实驱动：支付结果不能只依赖本地状态，必须基于链上交易验证
- 幂等优先：支付扫描、状态更新、通知发送必须幂等
- 异步解耦：监听、验证、核销、通知、Agent 总结彼此解耦
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
- MyBatis-Plus
- PostgreSQL
- Redis

选择理由：

- JDK 21 是 LTS 版本，适合当前项目长期演进
- 你已有其他项目在使用 JDK 21，可降低上下文切换成本
- Spring Boot 3.x 对 JDK 21 支持成熟
- Java + Spring Boot 适合账单、状态机、定时任务、审计日志等强业务系统
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

不建议过度依赖的场景：

- 简单 CRUD 接口
- 仍需严格控制并发边界的复杂事务逻辑
- 尚未验证线程模型收益的代码路径

推荐策略：

- Web 层保持 Spring Boot 默认实践
- 在外部 I/O 密集模块中使用虚拟线程执行器
- 保持线程模型清晰，不把“用了虚拟线程”当成架构卖点

### 3.3 异步与消息中间件建议

当前阶段明确建议：

- P0 不引入 Kafka
- P0 主路径采用：数据库状态表 + Spring Scheduler + Spring 异步执行器 + Outbox 事件表
- 若确实需要消息中间件，优先 RabbitMQ

推荐理由：

- 当前是模块化单体，不是多服务事件平台，Kafka 的运维和使用复杂度过高
- 当前异步任务主要是扫描、验证、通知、Agent 总结，RabbitMQ 更适合工作队列、延迟重试和低成本接入
- Kafka 更适合高吞吐、流式分析、多消费者事件平台，不是当前 MVP 的核心痛点

明确建议：

- 第一阶段：不强依赖消息中间件
- 第二阶段：若通知、Agent、Webhook 等异步任务明显增多，再引入 RabbitMQ
- Kafka 只在未来多服务拆分、海量交易事件流转时再考虑

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
- 限流与防重复提交

### 3.5 前端选型建议

推荐方案：

- React
- Vite
- TypeScript
- Ant Design
- TanStack Query
- React Router

选择理由：

- 你的当前产品主要是中后台和支付页，不是内容站，没必要优先上 Next.js
- Vite + React 对单独前端项目更简单，启动快、心智负担低，更适合黑客松周期
- Ant Design 很适合账单列表、筛选、表单、统计面板等中后台页面
- TanStack Query 适合处理后端 API 请求、缓存、刷新和状态同步

补充建议：

- 支付页支持二维码 + 支付链接
- 如需要钱包连接，可补充 `@solana/wallet-adapter-react`
- 钱包优先兼容：Phantom、Solflare、Backpack

### 3.6 Solana 接入建议

推荐方案：

- 使用托管 RPC 服务，例如 Helius 或 QuickNode
- 后端封装独立 `SolanaClient`
- 优先采用轮询扫描 + 交易详情查询的方式完成 MVP
- 支付请求采用 Solana 原生支付链接 / 二维码

原因：

- 托管 RPC 更稳定，适合黑客松 demo 和后续线上演示
- 轮询方案更容易控制、排查和重试
- 当前核心是支付闭环，不需要为了“实时感”过早上复杂订阅系统

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
- `merchant`：商家信息与商家设置
- `invoice`：账单创建、编辑、状态管理
- `payment`：支付地址、支付请求、支付状态查询
- `blockchain`：Solana RPC 封装、交易扫描、交易解析
- `verification`：支付验证、订单归因、状态判定
- `reconciliation`：核销处理、支付证明、异常处理
- `notification`：邮件、站内消息、后续 Webhook
- `dashboard`：统计查询、对账汇总、异常筛选
- `agent`：自然语言开单、异常解释、对账总结
- `common`：异常、工具类、基础枚举、审计字段、响应模型

### 4.3 逻辑分层

推荐分层如下：

- Controller：对外提供 REST API
- Application Service：编排业务流程
- Domain Service：封装核心业务规则与状态流转
- Infrastructure：数据库、Redis、Solana RPC、通知、消息中间件
- Scheduler / Worker：异步扫描、重试、提醒、汇总任务

---

## 5. 核心业务链路设计

### 5.1 主链路

主链路如下：

1. 商家创建 Invoice
2. 系统生成唯一收款地址
3. 系统生成支付链接 / 支付二维码
4. 客户使用 Solana 钱包支付 USDC
5. 后端扫描目标地址相关交易
6. 后端获取交易详情并解析 token transfer
7. 后端验证地址、币种、金额、有效期、幂等性
8. 后端更新 Invoice 状态并写入核销记录
9. 后端生成支付结果说明 / Payment Proof
10. 后端触发通知与对账更新
11. Agent 基于已验证结果提供解释或总结

### 5.2 为什么区分监听、验证、核销

这三步必须明确拆开：

- 监听：发现疑似支付
- 验证：判断该交易是否满足该 Invoice 的支付条件
- 核销：基于验证结果更新业务状态并沉淀结果

这样做的原因：

- 避免“地址收到了钱就直接判成功”
- 支持少付、多付、过期到账、错误币种等异常分支
- 便于做幂等、重试、异常回放
- 更符合 Solana 黑客松对链上验证闭环的期待

---

## 6. 支付接入与链上验证设计

### 6.1 支付请求设计

每张 Invoice 在创建后生成两类支付信息：

- 唯一收款地址
- Solana 原生支付请求

支付请求至少包含：

- recipient：收款地址
- amount：金额
- spl-token：USDC mint 地址
- label：商家名称
- message：订单摘要
- reference：可选的订单辅助标识

说明：

- MVP 中，订单归因仍然以“一单一地址”为主
- `reference` 作为增强信息，不作为唯一归因依据
- 支付页同时展示地址、金额、二维码和支付链接

### 6.2 支付监听方案

MVP 推荐方案：

- 使用定时任务按批扫描待支付 Invoice 的收款地址
- 通过 RPC 查询地址相关签名列表
- 对新增签名拉取交易详情
- 将交易写入 `payment_transaction` 并进入验证队列

推荐参数：

- 扫描周期：10 到 20 秒
- 每批扫描待支付账单数：100 到 500，按环境调整
- 地址扫描范围：仅 `PENDING`、`PARTIALLY_PAID`、`OVERPAID`、临近过期账单

### 6.3 链上验证规则

对每笔候选交易，需要完成以下验证：

- 交易状态是否成功
- 接收地址是否为该 Invoice 的收款地址
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

### 6.4 Payment Proof 设计

MVP 不上链上智能合约，但需要提供“可验证支付结果”。

Payment Proof 可作为系统内的支付凭证视图，至少包含：

- invoiceId
- txHash
- payerAddress
- receiverAddress
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

### 8.2 invoice

核心字段：

- `id`
- `merchant_id`
- `invoice_no`
- `customer_name`
- `amount`
- `currency`
- `chain`
- `description`
- `status`
- `expire_at`
- `paid_at`
- `created_at`
- `updated_at`

### 8.3 invoice_payment_account

核心字段：

- `id`
- `invoice_id`
- `payment_address`
- `mint_address`
- `expected_amount`
- `expire_at`
- `payment_link`
- `active_flag`
- `created_at`

### 8.4 payment_transaction

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `payer_address`
- `receiver_address`
- `amount`
- `currency`
- `mint_address`
- `block_time`
- `verification_result`
- `payment_status`
- `raw_payload`
- `created_at`

约束建议：

- `tx_hash` 唯一索引
- `invoice_id + tx_hash` 联合索引
- `receiver_address + block_time` 普通索引

### 8.5 reconciliation_record

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `reconciliation_status`
- `result_message`
- `exception_tags`
- `processed_at`
- `created_at`

### 8.6 payment_proof

核心字段：

- `id`
- `invoice_id`
- `tx_hash`
- `proof_type`
- `proof_payload`
- `created_at`

### 8.7 notification_record

核心字段：

- `id`
- `invoice_id`
- `notification_type`
- `receiver`
- `send_status`
- `retry_count`
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
- `next_retry_at`
- `created_at`

用途：

- 解耦通知、Agent 总结、后续 Webhook
- 在不引入 Kafka 的情况下支撑可靠异步事件处理

---

## 9. API 设计

### 9.1 认证接口

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### 9.2 Invoice 接口

- `POST /api/invoices`：创建账单
- `PUT /api/invoices/{id}`：编辑账单
- `GET /api/invoices/{id}`：查询详情
- `GET /api/invoices`：分页查询
- `POST /api/invoices/{id}/activate`：草稿转待支付
- `POST /api/invoices/{id}/reconcile`：手动触发核销

### 9.3 支付接口

- `GET /api/invoices/{id}/payment-info`：支付地址、金额、二维码、支付链接
- `GET /api/invoices/{id}/payment-status`：支付状态
- `GET /api/invoices/{id}/payment-proof`：支付凭证

### 9.4 Dashboard 接口

- `GET /api/dashboard/summary`
- `GET /api/dashboard/invoices/status`
- `GET /api/dashboard/invoices/exceptions`
- `GET /api/dashboard/reconciliation-summary`

### 9.5 Agent 接口

- `POST /api/agent/create-invoice`
- `POST /api/agent/reminder-message`
- `POST /api/agent/exception-explain`
- `POST /api/agent/reconciliation-summary`

### 9.6 通用接口约定

推荐统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误码建议按分类设计：

- `AUTH_*`
- `INVOICE_*`
- `PAYMENT_*`
- `RECONCILIATION_*`
- `AGENT_*`
- `SYSTEM_*`

---

## 10. 异步任务设计

### 10.1 必备定时任务

- `InvoiceExpireJob`：扫描到期账单并更新状态
- `PaymentScanJob`：扫描待支付账单相关交易
- `PaymentVerifyJob`：处理待验证交易
- `NotificationDispatchJob`：发送待发送通知
- `OutboxDispatchJob`：分发 outbox 事件
- `ReminderJob`：生成逾期提醒任务

### 10.2 推荐执行方式

第一阶段：

- Spring Scheduler 扫描数据库任务表 / outbox_event
- 每类任务单独线程池
- Redis 锁避免多实例重复执行
- 对 I/O 密集任务可评估使用虚拟线程执行器

第二阶段：

- 需要更好的削峰、重试和异步隔离时，引入 RabbitMQ

### 10.3 为什么不先上 Kafka

- 当前吞吐量不大
- 当前只有单体应用，Kafka 价值不明显
- Kafka 对本地开发、测试、部署、排障都更重
- 黑客松 MVP 重点是业务闭环，不是事件平台建设

---

## 11. 安全与权限设计

### 11.1 认证方案

推荐：

- Spring Security + JWT

说明：

- 商家后台使用 JWT 访问 API
- 可先做单商家 demo 模式，再补完整注册登录
- 黑客松 demo 可以预置测试账号

### 11.2 权限边界

- 商家只能查看自己的 Invoice、交易、通知和统计
- 公共支付页无需登录，但只能查看单张账单的公开支付信息
- Agent 接口默认只能访问当前商家的数据

### 11.3 配置安全

需要保护的配置：

- 数据库连接串
- Redis 密码
- RPC API Key
- 邮件服务密钥
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
- 通知发送日志
- Agent 调用日志

建议日志字段：

- `traceId`
- `merchantId`
- `invoiceId`
- `txHash`
- `eventType`
- `status`

### 12.2 监控指标

建议统计：

- 待支付账单数
- 已支付账单数
- 异常账单数
- 支付扫描耗时
- 支付验证成功率
- 通知成功率
- Agent 请求耗时

### 12.3 部署建议

开发和演示环境推荐：

- 前端：Vercel 或静态托管
- 后端：Docker 部署到云主机
- PostgreSQL：托管或单机容器
- Redis：托管或单机容器
- RabbitMQ：暂不必备

本地开发推荐 Docker Compose：

- app
- postgres
- redis
- rabbitmq（可选）

---

## 13. 测试策略

### 13.1 单元测试

重点覆盖：

- Invoice 状态流转
- 支付验证规则
- 异常标签判定
- 幂等处理逻辑
- Agent 输入解析

### 13.2 集成测试

重点覆盖：

- 创建账单 -> 生成支付信息
- 扫描交易 -> 验证 -> 核销
- 少付、多付、过期到账
- 重复扫描同一 txHash
- 通知发送与失败重试

### 13.3 验收测试

必须覆盖：

- 正常支付闭环
- 少付闭环
- 多付闭环
- 过期到账闭环
- Agent 自然语言开单
- Agent 异常解释

---

## 14. 开发优先级建议

### 14.1 P0

- Merchant 基础认证
- Invoice 创建与列表
- 唯一收款地址生成
- 支付链接 / 二维码生成
- PaymentScanJob
- 交易详情解析
- 支付验证与核销
- 异常单展示
- Dashboard 基础汇总

### 14.2 P1

- Payment Proof 详情页
- 邮件通知
- Agent 自然语言开单
- Agent 异常解释
- Agent 对账总结

### 14.3 P2

- RabbitMQ 接入
- Webhook
- 多钱包更深兼容
- 更复杂权限体系
- 自动归集

---

## 15. 最终建议

当前项目最适合的落地方式是：

- 架构采用模块化单体
- 后端采用 JDK 21 + Spring Boot + MyBatis-Plus + PostgreSQL + Redis
- 前端采用 React + Vite + TypeScript + Ant Design
- 异步优先使用 Scheduler + Outbox，不急着上 Kafka
- 如异步复杂度提高，再引入 RabbitMQ
- 产品主线坚持 billing -> payment -> verification -> reconciliation
- Agent 作为增强层，不与支付判定主链路耦合

一句话总结：

**先做一个稳定、清晰、可演示的 Solana Stablecoin Billing & Reconciliation MVP，再逐步演进为更完整的商家收款基础设施。**
