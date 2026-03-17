# StableFlow 开发任务表

## 1. 目标

本文档用于把 `docs/technical-design.md` 和 `docs/implementation-guide.md` 收敛成可执行开发任务，默认服务于当前 MVP。

当前开发目标只有一个：

**优先跑通 `固定地址 + reference` 的支付闭环。**

主链路如下：

1. 商家登录
2. 商家配置固定收款地址
3. 商家创建 Invoice
4. 系统生成 payment request
5. 支付页展示支付信息
6. 后端扫描链上交易
7. 后端完成 reference 归因与支付验证
8. 后端完成核销并生成 Payment Proof
9. 商家查看状态与基础汇总

---

## 2. 使用说明

- 按任务顺序开发，不建议跨阶段并行铺太多模块
- 每个任务都有交付物和完成标准
- P0 只做最小可演示闭环
- P0.5 做幂等、异常、定时任务补强
- P1 再做 Agent、异常页增强、Webhook 等扩展能力

任务状态建议使用：

- `TODO`
- `DOING`
- `DONE`
- `BLOCKED`

---

## 3. 总体排期建议

| 阶段 | 目标 | 建议优先级 |
| --- | --- | --- |
| M0 | 工程基础与数据库收口 | P0 |
| M1 | 认证与商家固定地址配置 | P0 |
| M2 | Invoice 与 payment request | P0 |
| M3 | Solana 扫描接入 | P0 |
| M4 | 验证、核销、Payment Proof | P0 |
| M5 | Dashboard 与公共支付页 | P0 |
| M6 | 定时任务补强与 outbox | P0.5 |
| M7 | 异常页、Agent、Webhook | P1 |

推荐执行顺序：

1. M0 -> M1 -> M2
2. M3 -> M4
3. M5
4. M6
5. M7

---

## 4. M0 工程基础与数据库收口

### T001 配置分层与环境变量收口

- 状态：`DONE`

- 优先级：P0
- 依赖：无
- 任务说明：整理 `application.yml`、`application-dev.yml`、环境变量读取方式
- 交付物：
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-dev.yml`
- 完成标准：
  - 数据库、Redis、JWT、RPC 配置支持环境变量
  - 不在仓库中提交真实密钥
  - 显式关闭自动建表

### T002 Flyway 迁移一致性检查

- 状态：`DONE`

- 优先级：P0
- 依赖：T001
- 任务说明：确认数据库 migration 与技术方案一致
- 交付物：
  - `db/migration` 下现有脚本检查结果
  - 必要时新增迁移脚本
- 完成标准：
  - `merchant_payment_config`、`invoice`、`invoice_payment_request`、`payment_transaction`、`reconciliation_record`、`payment_proof`、`outbox_event` 均与设计一致
  - 时间字段统一使用 `TIMESTAMPTZ`

### T003 补充幂等与排障字段

- 状态：`DONE`

- 优先级：P0
- 依赖：T002
- 任务说明：为核心表补充支撑幂等和重试的字段与约束
- 交付物：
  - 新 migration
- 完成标准：
  - `reconciliation_record(invoice_id, tx_hash)` 唯一
  - `outbox_event` 具备 `last_error`、`updated_at`
  - 核心索引覆盖 `reference_key`、`recipient_address`、`status`

### T004 公共基础设施代码

- 状态：`DONE`

- 优先级：P0
- 依赖：T001
- 任务说明：先建立项目公共代码，避免后续重复造轮子
- 交付物：
  - 统一响应模型
  - 全局异常处理
  - 基础错误码
  - traceId 透传
- 完成标准：
  - 控制器返回格式统一
  - 业务异常可控
  - 日志可按请求链路排查

---

## 5. M1 认证与商家固定地址配置

### T101 实现最小登录闭环

- 状态：`DONE`

- 优先级：P0
- 依赖：T004
- 任务说明：完成商家后台最小登录能力
- 交付物：
  - `POST /api/auth/login`
  - JWT 生成与校验
  - 当前登录商家上下文
- 完成标准：
  - 登录成功后可拿到 token
  - 受保护接口可以识别当前商家

### T102 商家固定收款地址配置接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T101
- 任务说明：支持商家配置和查看固定 Solana USDC 收款地址
- 交付物：
  - `POST /api/merchant/payment-config`
  - `GET /api/merchant/payment-config`
- 完成标准：
  - 一个商家只能维护一条有效配置
  - 地址、链、mint 基础校验通过

### T103 固定地址所有权验证扩展位

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T102
- 任务说明：为后续钱包签名验证保留扩展能力
- 交付物：
  - 设计说明
  - 挑战码/签名校验接口预留
- 完成标准： 
  - 当前不阻塞 P0
  - 后续可平滑接入地址所有权证明

---

## 6. M2 Invoice 与 payment request

### T201 创建 Invoice

- 状态：`DONE`

- 优先级：P0
- 依赖：T102
- 任务说明：实现创建账单接口和基础字段校验
- 交付物：
  - `POST /api/invoices`
  - `CreateInvoiceRequest`
  - `InvoiceService`
- 完成标准：
  - 可保存商家自己的账单
  - 自动生成 `invoice_no`
  - 自动生成 `public_id`

### T202 Invoice 列表与详情

- 状态：`DONE`

- 优先级：P0
- 依赖：T201
- 任务说明：实现账单分页查询与详情查询
- 交付物：
  - `GET /api/invoices`
  - `GET /api/invoices/{id}`
- 完成标准：
  - 仅能查询当前商家的账单
  - 列表支持按状态筛选

### T203 生成 payment request

- 状态：`DONE`

- 优先级：P0
- 依赖：T201
- 任务说明：创建账单时自动生成支付请求
- 交付物：
  - `reference_key`
  - `invoice_payment_request`
  - payment link 生成逻辑
- 完成标准：
  - `reference_key` 全局唯一
  - payment request 使用商家固定地址快照
  - 保存 `recipient_address`、`mint_address`、`expected_amount`

### T204 支付信息查询接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T203
- 任务说明：提供支付页和后台可复用的支付信息查询能力
- 交付物：
  - `GET /api/invoices/{id}/payment-info`
- 完成标准：
  - 返回金额、地址、reference、payment link、expireAt
  - 支持后台详情页和公共支付页复用

### T205 Invoice 激活与过期前置规则

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T201
- 任务说明：明确 `DRAFT -> PENDING` 及过期前的行为规则
- 交付物：
  - `POST /api/invoices/{id}/activate`
  - 状态流转规则实现
- 完成标准：
  - 草稿账单不会被扫描
  - 激活后进入待支付流程

---

## 7. M3 Solana 扫描接入

### T301 封装 `SolanaClient`

- 状态：`DONE`

- 优先级：P0
- 依赖：T001
- 任务说明：提供统一的 Solana RPC 调用封装
- 交付物：
  - 查询地址相关签名
  - 查询交易详情
  - 解析 token transfer、recipient、mint、amount、reference
- 完成标准：
  - 给定交易签名可以解析出验证所需核心字段

### T302 设计扫描游标策略

- 状态：`DONE`

- 优先级：P0
- 依赖：T301
- 任务说明：避免每次扫描都全量拉取历史交易
- 交付物：
  - 地址扫描游标设计
  - 批次大小与扫描周期配置
- 完成标准：
  - 能按商家固定地址增量扫描
  - 支持重复执行但不会造成全量回扫

### T303 实现 `PaymentScanJob`

- 状态：`DONE`

- 优先级：P0
- 依赖：T302
- 任务说明：扫描候选交易并落库
- 交付物：
  - `PaymentScanJob`
  - `PaymentScanService`
  - `payment_transaction` 落库逻辑
- 完成标准：
  - 同一 `tx_hash` 不重复插入
  - 能记录候选交易原始 payload
  - 能解析出 `reference_key`

### T304 扫描任务锁与并发控制

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T303
- 任务说明：避免多实例或重复调度导致重复扫描
- 交付物：
  - Redis 锁或等价控制方案
- 完成标准：
  - 同一时刻同一任务不会被重复执行

---

## 8. M4 验证、核销与 Payment Proof

### M4 技术方案补充

当前代码已经完成以下基础能力：

- 商家登录
- 商家固定收款地址配置
- Invoice 创建、列表、详情
- payment request 生成
- Solana 交易解析
- 按收款地址增量扫描候选交易
- 候选交易落库到 `payment_transaction`

当前真正未闭环的部分，是“扫描到交易之后如何认账”。因此 M4 的重点不是继续扩扫描，而是完成 verification、reconciliation 与 Payment Proof。

推荐按以下顺序实现：

1. 先做 `T401`，把 `reference_key -> invoice_payment_request -> invoice` 的关联和验证规则落成代码
2. 再做 `T402`，把待验证交易批量处理起来
3. 再做 `T403`，完成核销和账单状态更新
4. 最后做 `T404`，输出 Payment Proof 视图

`T401` 第一版建议先只覆盖最小规则：

- `MISSING_REFERENCE`
- `INVALID_REFERENCE`
- `WRONG_CURRENCY`
- `PAID`
- `PARTIALLY_PAID`
- `OVERPAID`

实现顺序建议：

1. 用 `payment_transaction.reference_key` 命中 `invoice_payment_request`
2. 拿到目标 `invoice`
3. 校验 `mint_address`
4. 比较 `amount` 与 `expected_amount`
5. 输出统一验证结果

说明：

- 过期到账、重复支付、更早有效支付优先可以放在 `T401` 第一版跑通后再补
- `T304` 依然重要，但它解决的是扫描稳定性，不是支付闭环认账问题，因此优先级低于 `T401`

### T401 支付验证规则实现

- 状态：`DONE`

- 优先级：P0
- 依赖：T303
- 任务说明：将 reference 归因、金额、币种、时间窗口等规则落成代码
- 交付物：
  - `PaymentVerificationService`
  - 验证结果模型
- 完成标准：
  - 能输出 `PAID`、`PARTIALLY_PAID`、`OVERPAID`
  - 能识别 `WRONG_CURRENCY`、`MISSING_REFERENCE`、`INVALID_REFERENCE`
  - 能识别过期到账与重复支付

### T402 实现 `PaymentVerifyJob`

- 状态：`TODO`

- 优先级：P0
- 依赖：T401
- 任务说明：读取待验证交易并执行验证流程
- 交付物：
  - `PaymentVerifyJob`
  - 待验证交易处理逻辑
- 完成标准：
  - 任务可重复执行
  - 同一交易不会重复核销

### T403 核销流程实现

- 状态：`TODO`

- 优先级：P0
- 依赖：T402
- 任务说明：验证通过后更新账单状态并记录核销结果
- 交付物：
  - `ReconciliationService`
  - `reconciliation_record`
  - `invoice.status` 更新逻辑
- 完成标准：
  - 状态流转符合技术方案
  - 异常单有记录、有说明

### T404 生成 Payment Proof

- 状态：`TODO`

- 优先级：P0
- 依赖：T403
- 任务说明：沉淀支付结果证据视图
- 交付物：
  - `payment_proof`
  - `GET /api/invoices/{id}/payment-proof`
- 完成标准：
  - 商家可看到交易哈希、reference、验证结果、最终状态、异常标签

### T405 `InvoiceExpireJob`

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T201
- 任务说明：扫描到期账单并更新状态
- 交付物：
  - `InvoiceExpireJob`
- 完成标准：
  - 到期未支付账单可更新为 `EXPIRED`
  - 与支付验证逻辑不冲突

---

## 9. M5 Dashboard 与公共支付页

### T501 Dashboard 基础汇总接口

- 状态：`TODO`

- 优先级：P0
- 依赖：T403
- 任务说明：输出商家后台最小汇总能力
- 交付物：
  - `GET /api/dashboard/summary`
- 完成标准：
  - 返回总账单数、已支付数、异常数、收款总额

### T502 公共支付页查询接口

- 状态：`TODO`

- 优先级：P0
- 依赖：T204
- 任务说明：基于 `public_id` 提供公开可见的支付信息
- 交付物：
  - 面向公共支付页的查询接口
- 完成标准：
  - 不暴露数据库自增主键
  - 仅返回支付页所需信息

### T503 支付状态查询接口

- 状态：`TODO`

- 优先级：P0
- 依赖：T403
- 任务说明：供前端轮询支付结果
- 交付物：
  - `GET /api/invoices/{id}/payment-status`
- 完成标准：
  - 能返回当前支付状态、异常标签、最近处理时间

---

## 10. M6 Outbox 与可观测性补强

### T601 Outbox 事件分发

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T403
- 任务说明：为后续通知、Webhook、Agent 预留可靠异步事件机制
- 交付物：
  - `OutboxDispatchJob`
  - outbox 状态流转逻辑
- 完成标准：
  - 事件可重试
  - 失败原因可记录

### T602 关键日志与指标

- 状态：`TODO`

- 优先级：P0.5
- 依赖：T303
- 任务说明：完善支付链路日志和核心统计指标
- 交付物：
  - 扫描日志
  - 验证日志
  - 核销日志
  - 异常单日志
- 完成标准：
  - 日志中可按 `merchantId`、`invoiceId`、`reference`、`txHash` 排查

---

## 11. M7 P1 扩展任务

### T701 异常账单页

- 状态：`TODO`

- 优先级：P1
- 依赖：T403
- 任务说明：按异常标签和状态查看异常账单

### T702 Agent 自然语言开单

- 状态：`TODO`

- 优先级：P1
- 依赖：T201
- 任务说明：从自然语言生成账单草稿或创建账单

### T703 Agent 异常解释

- 状态：`TODO`

- 优先级：P1
- 依赖：T404
- 任务说明：基于已验证数据解释异常原因

### T704 Webhook / 外部事件通知

- 状态：`TODO`

- 优先级：P1
- 依赖：T601
- 任务说明：将支付结果和异常结果对外分发

---

## 12. 必写测试清单

### 12.1 单元测试

- `InvoiceStatus` 状态流转
- `reference_key` 归因规则
- 金额比较规则
- `MISSING_REFERENCE` 判定
- `INVALID_REFERENCE` 判定
- `WRONG_CURRENCY` 判定
- 重复处理幂等规则

### 12.2 集成测试

- 登录 -> 配置固定地址 -> 创建 Invoice
- 创建 Invoice -> 生成 payment request
- 扫描交易 -> 验证 -> 核销
- 少付 / 多付 / 过期到账
- 缺少 `reference`
- `reference` 不匹配
- 重复扫描同一 `tx_hash`

### 12.3 验收测试

- 商家可以配置固定收款地址
- 商家可以创建账单并获取支付链接
- 用户完成带 `reference` 的支付后，系统可核销成功
- 异常支付会进入异常结果视图
- 商家可以查看 Payment Proof 和基础汇总

---

## 13. 建议的首周开发顺序

如果现在立即开工，建议按下面顺序推进：

1. T001 -> T004
2. T101 -> T102
3. T201 -> T204
4. T301 -> T303
5. T401 -> T404
6. T501 -> T503

首周目标不是把所有模块都搭一遍，而是做到：

**从创建 Invoice 到生成 Payment Proof 的主链路可运行、可调试、可演示。**

---

## 14. 今日可直接开始的任务

如果你现在就准备进入编码，建议先从这 5 个任务开始：

1. T001 配置分层与环境变量收口
2. T003 补充幂等与排障字段
3. T101 实现最小登录闭环
4. T102 商家固定收款地址配置接口
5. T201 创建 Invoice

做到这一步后，再继续推进 payment request 和链上扫描，会最顺。
