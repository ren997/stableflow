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

- 状态：`DONE`

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
- 任务说明：实现账单列表与详情查询（分页能力由 T1006 单独补充）
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

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T201
- 任务说明：明确 `DRAFT -> PENDING` 及过期前的行为规则
- 交付物：
  - `POST /api/invoices/activate`
  - 状态流转规则实现
- 完成标准：
  - 草稿账单不会暴露支付信息，也不会进入验证核销流程
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

- 状态：`DONE`

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

- 状态：`DONE`

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

- 状态：`DONE`

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

- 状态：`DONE`

- 优先级：P0
- 依赖：T403
- 任务说明：沉淀支付结果证据视图
- 交付物：
  - `payment_proof`
  - `GET /api/invoices/{id}/payment-proof`
- 完成标准：
  - 商家可看到交易哈希、reference、验证结果、最终状态、异常标签

### T405 `InvoiceExpireJob`

- 状态：`DONE`

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

- 状态：`DONE`

- 优先级：P0
- 依赖：T403
- 任务说明：输出商家后台最小汇总能力
- 交付物：
  - `GET /api/dashboard/summary`
- 完成标准：
  - 返回总账单数、已支付数、异常数、收款总额

### T502 公共支付页查询接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T204
- 任务说明：基于 `public_id` 提供公开可见的支付信息
- 交付物：
  - 面向公共支付页的查询接口
- 完成标准：
  - 不暴露数据库自增主键
  - 仅返回支付页所需信息

### T503 支付状态查询接口

- 状态：`DONE`

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

- 状态：`DONE`

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

- 状态：`DONE`

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

## 13. 历史归档：早期启动建议

> 以下两节为项目初期编写的启动引导，所列任务大多已完成（M0–M4 P0 全部 DONE，M5 T501/T502/T503 已 DONE）。保留仅供回溯，**新线程请直接查看各任务的实际状态，不要按本节顺序执行**。

### 13.1 早期首周开发顺序（已过时）

1. T001 -> T004
2. T101 -> T102
3. T201 -> T204
4. T301 -> T303
5. T401 -> T404
6. T501 -> T503

### 13.2 早期启动任务（已过时）

1. T001 配置分层与环境变量收口
2. T003 补充幂等与排障字段
3. T101 实现最小登录闭环
4. T102 商家固定收款地址配置接口
5. T201 创建 Invoice

---

## 15. 遗漏补充任务

本节为对照 `requirements.md`、`technical-design.md`、`implementation-guide.md` 后发现的任务表遗漏，按类别分组。

### 15.1 后端 API 与功能补充

#### T801 商家注册接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T004
- 任务说明：补齐商家自助注册能力，作为当前 MVP 的默认入场方案
- 交付物：
  - `POST /api/auth/register`
- 完成标准：
  - 新商家可以通过注册方式进入系统
  - 密码使用 BCrypt 哈希存储

#### T802 当前登录用户信息接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T101
- 任务说明：前端需要获取当前登录商家信息用于页面展示和权限判断；当前接口已存在，但返回字段仍需补齐名称和状态
- 交付物：
  - `GET /api/auth/me`
- 完成标准：
  - 返回当前商家基本信息（名称、邮箱、状态）
  - 未登录时返回 401

#### T803 登出接口

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T101
- 任务说明：补充登出能力，清理 token 或前端状态
- 交付物：
  - `POST /api/auth/logout`
- 完成标准：
  - 登出后 token 失效或由前端清除

#### T804 Invoice 编辑接口

- 状态：`DONE`

- 优先级：P0
- 依赖：T201
- 任务说明：需求文档 8.2 明确要求"编辑账单"，当前任务表完全未覆盖
- 交付物：
  - `POST /api/invoices/update`
- 完成标准：
  - 仅 `DRAFT` 状态可编辑
  - 仅能编辑当前商家自己的账单

#### T805 手动触发核销接口

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T403
- 任务说明：技术方案 9.3 中定义了 `POST /api/invoices/{id}/reconcile`，用于异常单重试或手动触发核销
- 交付物：
  - `POST /api/invoices/reconcile`
- 完成标准：
  - 可对异常单手动触发重新核销
  - 操作幂等

#### T806 Dashboard 按状态分布查询

- 状态：`DONE`

- 优先级：P0
- 依赖：T501
- 任务说明：技术方案 9.5 中定义了 `GET /api/dashboard/invoices/status`，T501 只覆盖了汇总
- 交付物：
  - `POST /api/dashboard/invoices/status`
- 完成标准：
  - 返回各状态的账单数量分布

#### T807 Dashboard 异常账单筛选接口

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T501
- 任务说明：技术方案 9.5 中定义了 `GET /api/dashboard/invoices/exceptions`
- 交付物：
  - `POST /api/dashboard/invoices/exceptions`
- 完成标准：
  - 支持按异常标签筛选
  - 返回异常账单列表

#### T808 Dashboard 时间维度聚合

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T501
- 任务说明：需求 8.6 要求"按日、周、月汇总收款数据"，T501 仅有总量汇总
- 交付物：
  - `POST /api/dashboard/summary/trend`
- 完成标准：
  - 可按日、周、月粒度返回收款趋势数据

#### T809 Invoice 列表按异常标签筛选

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T202
- 任务说明：T202 只支持按状态筛选，需求 8.6 要求"按异常标签筛选账单"
- 交付物：
  - `POST /api/invoices/list` 增加 `exceptionTag` 筛选参数
- 完成标准：
  - 可按异常标签过滤账单列表

#### T810 Agent 对账总结

- 状态：`TODO`

- 优先级：P1
- 依赖：T501
- 任务说明：需求 8.7 和技术方案 9.6 均定义了对账总结能力，M7 中缺失此任务
- 交付物：
  - `POST /api/agent/reconciliation-summary`
- 完成标准：
  - Agent 可基于已验证数据输出对账总结
  - 结果与 Dashboard 统计口径一致

### 15.2 工程基础补充

#### T811 SpringDoc / Swagger UI 接入

- 状态：`DONE`

- 优先级：P0
- 依赖：T004
- 任务说明：技术方案 3.5 明确要求接入 SpringDoc + Swagger UI，用于联调和 Demo 演示
- 交付物：
  - SpringDoc 依赖引入与配置
  - Swagger UI 支持 Bearer JWT 鉴权调试
- 完成标准：
  - 启动后可访问 Swagger UI
  - 受保护接口可通过 Swagger 传入 token 调试

#### T812 CORS 跨域配置

- 状态：`DONE`

- 优先级：P0
- 依赖：T004
- 任务说明：`SecurityConfig` 中无 CORS 配置，前端 `localhost:5173` 调后端 `localhost:8080` 会被浏览器拦截
- 交付物：
  - SecurityConfig 中添加 `CorsConfigurationSource` Bean
  - 允许的 origins 从配置文件读取
- 完成标准：
  - 前端开发服务器可正常调用后端 API
  - 生产环境可配置允许的域名

#### T813 Solana RPC 异常重试与降级

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T301
- 任务说明：系统强依赖 Solana RPC，需要处理超时、限流和节点不可用
- 交付物：
  - RPC 调用超时配置
  - 重试策略（指数退避）
  - 降级日志与告警
- 完成标准：
  - RPC 暂时不可用时不会导致任务全部失败
  - 重试次数和间隔可配置

### 15.3 前端任务

#### T901 前端登录页

- 状态：`DONE`

- 优先级：P0
- 依赖：T101
- 任务说明：实现商家登录页面
- 交付物：
  - 登录表单
  - JWT token 存储与请求拦截
  - 未登录重定向
- 完成标准：
  - 登录成功后可进入后台
  - token 过期时自动跳转登录页

#### T902 前端收款地址配置页

- 状态：`DONE`

- 优先级：P0
- 依赖：T102, T901
- 任务说明：商家可在后台配置和查看固定收款地址
- 交付物：
  - 收款地址配置表单
  - 当前配置展示
- 完成标准：
  - 可保存和查看固定收款地址

#### T903 前端 Invoice 列表与创建页

- 状态：`DONE`

- 优先级：P0
- 依赖：T201, T202, T901
- 任务说明：商家可在后台创建账单和查看账单列表
- 交付物：
  - 账单创建表单
  - 账单列表页（支持状态筛选和分页）
- 完成标准：
  - 可创建账单并跳转详情
  - 列表可按状态筛选

#### T904 前端 Invoice 详情与支付信息页

- 状态：`DONE`

- 优先级：P0
- 依赖：T204, T903
- 任务说明：展示账单详情、支付信息、支付链接和二维码
- 交付物：
  - 账单详情展示
  - 支付信息与二维码展示
  - 支付状态实时轮询
- 完成标准：
  - 商家可查看支付信息并分享给客户
  - 支付状态可自动刷新

#### T905 前端公共支付页

- 状态：`DONE`

- 优先级：P0
- 依赖：T502
- 任务说明：基于 `public_id` 的公开支付页面，面向付款方，无需登录
- 交付物：
  - 无需登录的支付信息展示页
  - 支付金额、地址、二维码展示
  - 支付状态轮询
- 完成标准：
  - 通过支付链接可直接访问
  - 不暴露内部 ID 和商家敏感信息

#### T906 前端 Dashboard 汇总页

- 状态：`DONE`

- 优先级：P0
- 依赖：T501, T901
- 任务说明：商家后台首页，展示核心经营数据
- 交付物：
  - 总账单数、已支付数、异常数、收款总额卡片
  - 状态分布图表
- 完成标准：
  - 登录后首页可看到基础汇总数据

#### T907 前端 Payment Proof 展示

- 状态：`DONE`

- 优先级：P0
- 依赖：T404, T904
- 任务说明：在账单详情页展示 Payment Proof 信息
- 交付物：
  - 交易哈希、reference、验证结果、异常标签展示
  - 链上浏览器链接
- 完成标准：
  - 商家可查看完整支付证据

#### T908 前端异常账单页

- 状态：`TODO`

- 优先级：P1
- 依赖：T701, T901
- 任务说明：按异常标签和状态查看异常账单
- 交付物：
  - 异常账单列表页
  - 按异常标签筛选
- 完成标准：
  - 可快速定位和查看异常账单

### 15.4 测试执行任务

#### T951 核心单元测试

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T404
- 任务说明：将第 12.1 节测试清单转化为可执行的单元测试代码
- 交付物：
  - `InvoiceStatus` 状态流转测试
  - `reference_key` 归因规则测试
  - 金额比较、异常判定、幂等规则测试
- 完成标准：
  - 覆盖 12.1 节列出的所有单元测试场景
  - 测试可在 CI 中自动运行

#### T952 主链路集成测试

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T404
- 任务说明：将第 12.2 节测试清单转化为可执行的集成测试代码
- 交付物：
  - 登录 -> 配置地址 -> 创建 Invoice 集成测试
  - 扫描 -> 验证 -> 核销集成测试
  - 少付 / 多付 / 过期到账 / 缺少 reference 等异常场景测试
- 完成标准：
  - 覆盖 12.2 节列出的所有集成测试场景
  - 测试可独立运行、可重复执行

---

## 16. 工程实践遗漏补充

本节为对照实际代码审查后发现的工程实践、安全防护、数据完整性、可运维性等维度的遗漏，不在原始需求和设计文档中，但对 MVP 可用性和生产质量有直接影响。

### 16.1 Bug 修复与数据一致性

#### T1001 修复 Invoice.exceptionTags 类型与 DB JSONB 不匹配

- 状态：`DONE`

- 优先级：P0
- 依赖：T002
- 任务说明：数据库 `invoice.exception_tags` 定义为 `JSONB`，但 `Invoice.java` 中是 `String` 类型且使用逗号分隔格式；`ReconciliationRecord.java` 中同一字段是 `JsonNode` + `JacksonTypeHandler`。写入 JSONB 列时逗号分隔字符串不是合法 JSON，可能导致运行时异常
- 交付物：
  - `Invoice.java` 中 `exceptionTags` 改为 `JsonNode` + `JacksonTypeHandler`，`@TableName` 加 `autoResultMap = true`
  - `SingleReconciliationServiceImpl.mergeExceptionTags` 改为操作 `JsonNode`
  - `InvoiceServiceImpl.splitExceptionTags` 和 `PaymentProofServiceImpl.splitExceptionTags` 适配 `JsonNode`
- 完成标准：
  - Invoice 和 ReconciliationRecord 的 exceptionTags 类型和序列化方式统一
  - 核销写入和账单查询不会因类型不匹配报错

#### T1002 payment_proof 表补充唯一约束

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T002
- 任务说明：`reconciliation_record` 有 `(invoice_id, tx_hash)` 唯一约束，但 `payment_proof` 表无此约束，`saveIfAbsent` 为"先查再插"非原子操作，并发下可能重复插入
- 交付物：
  - 新 Flyway migration 添加 `payment_proof(invoice_id, tx_hash)` 唯一约束
- 完成标准：
  - 数据库层面防止同一 invoice + txHash 重复生成凭证

#### T1003 Invoice 并发核销乐观锁

- 状态：`TODO`

- 优先级：P1
- 依赖：T403
- 任务说明：同一 invoice 多笔不同 txHash 的交易并发核销时，`updateById` 无版本号控制，可能出现"后写覆盖前写"导致状态不一致
- 交付物：
  - Invoice 表新增 `version` 字段的 Flyway migration
  - `Invoice.java` 添加 `@Version` 注解
  - MyBatis-Plus 乐观锁插件配置
- 完成标准：
  - 并发更新同一 invoice 时，乐观锁冲突可被检测并重试

### 16.2 基础功能补全

#### T1004 种子数据

- 状态：`BLOCKED`

- 优先级：P0
- 依赖：T002
- 任务说明：当前主线已改为 T801 注册方案，因此本任务不再阻塞 MVP；仅在需要预置演示账号时再启用
- 交付物：
  - 新 Flyway migration 插入测试商家（BCrypt 密码哈希）
- 完成标准：
  - 应用启动后可用测试账号直接登录

#### T1005 （已合并到 T812）

#### T1006 Invoice 列表分页

- 状态：`DONE`

- 优先级：P0
- 依赖：T202
- 任务说明：`listInvoices` 返回全量 `List`，无 page/size 参数，账单量大时前端卡死
- 交付物：
  - `GET /api/invoices` 增加 `page`、`size` 参数
  - 使用 MyBatis-Plus `IPage` 分页
  - 返回分页结果（列表 + 总数）
- 完成标准：
  - 默认分页，支持按页查询
  - 兼容现有 status 筛选参数

#### T1007 Invoice 取消/作废

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T201
- 任务说明：商家创建错误账单后无法取消，`InvoiceStatusEnum` 无 `CANCELLED` 状态
- 交付物：
  - `InvoiceStatusEnum` 新增 `CANCELLED`
  - `POST /api/invoices/cancel`
- 完成标准：
  - 仅 `DRAFT` 或 `PENDING` 状态可取消
  - 已取消账单不会暴露支付信息，也不会进入验证和核销归账流程

#### T1008 Invoice 状态流转集中校验

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T201
- 任务说明：状态变更逻辑分散在 `InvoiceServiceImpl` 和 `SingleReconciliationServiceImpl`，无 `canTransition(from, to)` 校验，理论上可从 `PAID` 改成 `PARTIALLY_PAID`
- 交付物：
  - `InvoiceStatusEnum` 中增加合法流转规则（`allowedTransitions`）
  - 状态变更处统一调用校验方法
- 完成标准：
  - 非法状态流转会抛出异常
  - 所有状态变更点收敛到统一校验

### 16.3 安全防护

#### T1009 输入校验补全

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T201
- 任务说明：`CreateInvoiceRequestDto` 中 `customerName` 仅 `@NotBlank` 无 `@Size`，`description` 无长度限制但数据库是 `VARCHAR(512)`，超长输入直接抛数据库异常
- 交付物：
  - `customerName` 添加 `@Size(max = 128)`
  - `description` 添加 `@Size(max = 512)`
  - `currency`、`chain` 添加 `@Size(max = 16)` / `@Size(max = 32)`
  - 其他 DTO 同步检查
- 完成标准：
  - 输入超长时返回 400 友好提示而非 500 数据库异常

#### T1010 GlobalExceptionHandler 补充 HttpMessageNotReadableException

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T004
- 任务说明：前端发送格式错误的 JSON（如 amount 传字符串）时进入通用 Exception 处理，返回 500 且可能泄露堆栈信息
- 交付物：
  - `GlobalExceptionHandler` 新增 `@ExceptionHandler(HttpMessageNotReadableException.class)`
  - 返回 400 和友好错误信息
- 完成标准：
  - JSON 格式错误不再返回 500
  - 错误响应不暴露内部堆栈

#### T1011 登录接口限流

- 状态：`TODO`

- 优先级：P1
- 依赖：T101
- 任务说明：`/api/auth/login` 无任何限流措施，可被暴力破解密码
- 交付物：
  - 基于 Redis 的登录失败次数限制（如同一邮箱 5 次/分钟锁定）
- 完成标准：
  - 超过阈值后返回限流提示
  - 限流窗口可配置

#### T1012 公共支付页接口限流

- 状态：`TODO`

- 优先级：P1
- 依赖：T502
- 任务说明：`GET /api/pay/{publicId}` 对外开放无限流，虽然 publicId 使用 UUID 熵足够，但 404 vs 200 的差异仍暴露信息
- 交付物：
  - IP 级别限流
- 完成标准：
  - 单 IP 高频请求被限制

### 16.4 可观测性与运维

#### T1013 logback-spring.xml 与 traceId 输出

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T004
- 任务说明：`TraceIdFilter` 已将 traceId 放入 MDC，但项目无 `logback-spring.xml`，使用 Spring Boot 默认日志格式，traceId 实际不会出现在日志中
- 交付物：
  - `backend/src/main/resources/logback-spring.xml`
  - 日志 pattern 中加入 `%X{traceId}`
- 完成标准：
  - 每条日志可按 traceId 排查请求链路

#### T1014 JWT 过期时间可配置

- 状态：`DONE`

- 优先级：P0.5
- 依赖：T101
- 任务说明：`JwtTokenProvider` 中 `TOKEN_TTL = Duration.ofHours(12)` 硬编码，不同环境无法调整
- 交付物：
  - `SecurityProperties` 新增 `tokenTtl` 字段
  - `application.yml` 新增 `stableflow.security.token-ttl` 配置项
  - `JwtTokenProvider` 从配置读取
- 完成标准：
  - JWT 过期时间可通过配置文件或环境变量调整

#### T1015 健康检查端点

- 状态：`TODO`

- 优先级：P1
- 依赖：T001
- 任务说明：未引入 `spring-boot-starter-actuator`，容器化部署或负载均衡器需要 `/actuator/health` 做存活探测
- 交付物：
  - `pom.xml` 引入 actuator 依赖
  - `application.yml` 配置暴露 health 和 info 端点
  - SecurityConfig 放行 `/actuator/**`
- 完成标准：
  - `/actuator/health` 可正常返回应用状态

#### T1016 Docker Compose 本地开发环境

- 状态：`TODO`

- 优先级：P1
- 依赖：T001
- 任务说明：本地开发需要手动安装 PostgreSQL 和 Redis，新人上手成本高
- 交付物：
  - 项目根目录 `docker-compose.yml`
  - 包含 PostgreSQL 和 Redis 服务
  - 端口和密码与 `application.yml` 默认值一致
- 完成标准：
  - `docker compose up -d` 即可拉起全部依赖服务
