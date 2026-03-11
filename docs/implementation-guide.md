# StableFlow 开发落地手册

## 1. 文档目标

本文档是面向开发阶段的落地手册，用于把 PRD 和技术方案进一步收敛为“可以直接开工”的实现清单。

本文档解决以下问题：

- 后端和前端项目目录该如何组织
- 数据库如何初始化，Flyway 应该如何接入
- 第一批表该如何设计与建表
- 第一条主业务链路的接口、任务和数据流应该如何落地
- P0 阶段推荐先实现哪些代码骨架

---

## 2. Flyway 选型建议

结论：**推荐使用 Flyway。**

对于当前项目，Flyway 比手写 SQL 初始化脚本更适合，原因如下：

- 数据库变更可版本化管理，适合你后续频繁调整表结构
- 适合单体 Spring Boot 项目，接入简单
- 与 PostgreSQL 配合成熟稳定
- 可以把“初始化表结构”“新增索引”“补充字段”“修正数据”都纳入统一迁移流程
- 黑客松阶段虽然节奏快，但越快越需要避免手工改库带来的环境漂移

当前阶段建议：

- 使用 Flyway 管理全部 DDL
- 不在项目里混用手工 SQL 初始化和 ORM 自动建表
- Spring Boot 关闭自动建表，统一以 Flyway 为准

---

## 3. 推荐项目结构

### 3.1 后端项目结构

```text
stableflow/
  docs/
    requirements.md
    technical-design.md
    implementation-guide.md
  backend/
    pom.xml
    src/main/java/com/stableflow/
      StableFlowApplication.java
      system/
        api/
        config/
        security/
        exception/
        utils/
        infrastructure/
      auth/
      merchant/
        controller/
        service/
        mapper/
        entity/
        dto/
        vo/
        enums/
      invoice/
        controller/
        service/
        mapper/
        entity/
        dto/
        vo/
        enums/
      payment/
        controller/
        service/
        mapper/
        entity/
        dto/
        vo/
        enums/
      blockchain/
        client/
        service/
        dto/
        enums/
      verification/
        service/
        dto/
        enums/
      reconciliation/
        controller/
        service/
        mapper/
        entity/
        dto/
        vo/
        enums/
      dashboard/
        controller/
        service/
        mapper/
        dto/
        vo/
      agent/
        controller/
        service/
        dto/
        vo/
      job/
    src/main/resources/
      application.yml
      application-dev.yml
      application-prod.yml
      db/migration/
      mapper/
  frontend/
    package.json
    vite.config.ts
    src/
      app/
      pages/
      components/
      features/
      services/
      hooks/
      router/
      styles/
```

说明：

- `docs/` 放所有产品与技术文档
- `backend/` 放 Spring Boot 应用
- `frontend/` 放 React 应用
- 当前阶段不要把后端拆成多个服务仓库
- `system/` 放系统级通用能力，不承载核心业务规则
- `auth/` 单独作为认证入口模块
- `merchant`、`invoice`、`payment`、`reconciliation` 等按业务域拆分
- `job/` 统一放定时任务和后台作业入口

### 3.2 后端包结构建议

推荐采用“顶层按系统级 / 业务域拆分，模块内再按技术分层”的方式。

系统级目录：

- `system.api`
- `system.config`
- `system.security`
- `system.exception`
- `system.utils`
- `system.infrastructure`

业务与业务相关目录：

- `auth`
- `merchant`
- `invoice`
- `payment`
- `blockchain`
- `verification`
- `reconciliation`
- `dashboard`
- `agent`
- `job`

说明：

- 优先先做 `auth`、`merchant`、`invoice`、`payment`、`blockchain`、`verification`、`reconciliation`、`dashboard`
- `agent` 放到 P1 再展开
- `notification` 放到 P1/P2 再补，不作为当前主链路阻塞项
- 模块内部统一采用 `controller/service/mapper/entity/dto/vo/enums` 分层
- 请求参数对象统一以 `Dto` 结尾，例如 `LoginRequestDto`
- 返回结果对象统一以 `Vo` 结尾，例如 `InvoiceDetailVo`
- 数据库持久化对象使用 `entity`
- 枚举统一放在各模块的 `enums`

---

## 4. 后端初始化建议

### 4.1 Maven 依赖建议

P0 推荐依赖：

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `spring-boot-starter-data-redis`
- `mybatis-plus-spring-boot3-starter`
- `postgresql`
- `flyway-core`
- `flyway-database-postgresql`
- `lombok`
- `jjwt` 或同类 JWT 库

可选依赖：

- `spring-boot-starter-mail`：通知增强阶段再加
- `spring-boot-starter-amqp`：后续引入 RabbitMQ 时再加
- Solana Java SDK 或 HTTP 封装依赖：根据最终接入方式选择

### 4.2 application.yml 建议

建议预留以下配置分组：

- `spring.datasource`
- `spring.data.redis`
- `spring.flyway`
- `security.jwt`
- `solana.rpc`
- `stableflow.payment`
- `agent.llm`

### 4.3 基础配置建议

- Spring Boot profile：`dev`、`prod`
- 日志格式统一 JSON 或至少统一带 traceId
- 所有时间统一使用 UTC 存储，前端按用户时区展示
- 数据库时间字段统一使用 `TIMESTAMPTZ`
- 金额统一使用 `BigDecimal`，并封装统一比较逻辑
- 公共支付页使用 `public_id` 作为外部标识

---

## 5. Flyway 迁移策略

### 5.1 目录约定

Flyway 脚本目录：

```text
backend/src/main/resources/db/migration/
```

### 5.2 文件命名规则

推荐命名：

```text
V1__init_merchant_invoice_and_payment_request.sql
V2__init_payment_transaction.sql
V3__init_reconciliation_and_outbox.sql
V4__add_core_indexes.sql
```

### 5.3 初期迁移拆分建议

建议第一阶段至少拆成这些 migration：

- `V1__init_merchant_invoice_and_payment_request.sql`
- `V2__init_payment_transaction.sql`
- `V3__init_reconciliation_and_outbox.sql`
- `V4__add_core_indexes.sql`

### 5.4 迁移实践建议

- 不要修改已执行过的历史 migration
- 表结构变更通过新增 migration 完成
- 对演示环境也保持同样的迁移顺序
- 若需要修复数据，单独新增修复 migration

---

## 6. 第一批数据库表建议

P0 建议先建 7 张核心表：

- `merchant`
- `merchant_payment_config`
- `invoice`
- `invoice_payment_request`
- `payment_transaction`
- `reconciliation_record`
- `payment_proof`
- `outbox_event`

### 6.1 merchant

用途：商家主体与登录归属。

关键字段建议：

- `id`
- `merchant_name`
- `email`
- `password_hash`
- `status`
- `created_at`
- `updated_at`

### 6.2 merchant_payment_config

用途：记录商家的固定收款地址配置。

关键字段建议：

- `id`
- `merchant_id`
- `wallet_address`
- `mint_address`
- `chain`
- `active_flag`
- `created_at`
- `updated_at`

### 6.3 invoice

用途：账单主表。

关键字段建议：

- `id`
- `public_id`
- `merchant_id`
- `invoice_no`
- `customer_name`
- `amount` numeric(36, 6)
- `currency`
- `chain`
- `description`
- `status`
- `exception_tags` jsonb
- `expire_at`
- `paid_at`
- `created_at`
- `updated_at`

### 6.4 invoice_payment_request

用途：记录 invoice 与 payment request 的映射关系。

关键字段建议：

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

### 6.5 payment_transaction

用途：记录链上识别到的交易详情。

关键字段建议：

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
- `raw_payload` jsonb
- `created_at`

### 6.6 reconciliation_record

用途：记录核销动作和结果。

关键字段建议：

- `id`
- `invoice_id`
- `tx_hash`
- `reconciliation_status`
- `result_message`
- `exception_tags` jsonb
- `processed_at`
- `created_at`

建议增加幂等约束：

- `invoice_id + tx_hash` 唯一

### 6.7 payment_proof

用途：记录可展示、可追踪的支付结果。

关键字段建议：

- `id`
- `invoice_id`
- `tx_hash`
- `proof_type`
- `proof_payload` jsonb
- `created_at`

### 6.8 outbox_event

用途：承接后续异步事件。

关键字段建议：

- `id`
- `event_type`
- `aggregate_type`
- `aggregate_id`
- `payload` jsonb
- `status`
- `retry_count`
- `last_error`
- `next_retry_at`
- `created_at`
- `updated_at`

---

## 7. 第一条主链路落地方案

### 7.1 P0 要先打通的链路

优先实现这一条：

1. 商家登录
2. 配置固定收款地址
3. 创建 Invoice
4. 生成 payment request
5. 展示支付页
6. 扫描链上交易
7. 验证交易
8. 核销 Invoice
9. 展示 Payment Proof
10. Dashboard 汇总更新

这条链路跑通后，项目就具备可演示性。

### 7.2 建议先开发的接口

第一批建议实现：

- `POST /api/auth/login`
- `POST /api/merchant/payment-config`
- `GET /api/merchant/payment-config`
- `POST /api/invoices`
- `GET /api/invoices`
- `GET /api/invoices/{id}`
- `GET /api/invoices/{id}/payment-info`
- `GET /api/invoices/{id}/payment-status`
- `GET /api/invoices/{id}/payment-proof`
- `GET /api/dashboard/summary`

### 7.3 后端执行顺序建议

推荐开发顺序：

1. `auth` 基础登录
2. `merchant` 固定收款地址配置
3. `invoice` 创建、列表、详情
4. `payment` 支付请求生成逻辑
5. `blockchain` RPC 封装
6. `PaymentScanJob`
7. `verification` 规则实现
8. `reconciliation` 状态更新与 Payment Proof
9. `dashboard` 统计查询
10. `agent` 接口封装

### 7.4 核心数据流

#### 创建账单

- Controller 接收创建请求
- Service 校验参数
- 创建 invoice 主记录
- 生成 `invoice_no` 和 `public_id`
- 为该 Invoice 生成 `reference_key`
- 从商家固定收款地址配置中读取 `recipient_address`
- 生成 `payment_link`
- 保存 `invoice_payment_request`
- 返回支付信息

#### 支付扫描

- `PaymentScanJob` 扫描目标商家固定地址相关候选交易
- 拉取交易详情并解析 `reference`
- 落库到 `payment_transaction`
- 写入待验证任务或 outbox

#### 支付验证

- `PaymentVerifyJob` 读取未验证交易
- 校验收款地址、reference、币种、金额、时间、幂等性
- 生成验证结果
- 写入 `reconciliation_record`
- 更新 `invoice.status`
- 生成 `payment_proof`
- 更新 dashboard 聚合结果

---

## 8. 核心代码骨架建议

### 8.1 先定义的枚举

建议优先定义：

- `InvoiceStatus`
- `ExceptionTag`
- `VerificationResult`
- `OutboxEventStatus`

### 8.2 先定义的 DTO

建议优先定义：

- 请求 DTO：
  - `LoginRequestDto`
  - `MerchantPaymentConfigRequestDto`
  - `CreateInvoiceRequestDto`
- 返回 VO：
  - `InvoiceDetailVo`
  - `PaymentInfoVo`
  - `PaymentStatusVo`
  - `PaymentProofVo`
  - `DashboardSummaryVo`

### 8.3 先定义的核心服务接口

建议优先抽象：

- `MerchantPaymentConfigService`
- `InvoiceService`
- `PaymentRequestService`
- `SolanaClient`
- `PaymentScanService`
- `PaymentVerificationService`
- `ReconciliationService`
- `PaymentProofService`
- `DashboardQueryService`

### 8.4 先实现的定时任务

建议优先实现：

- `PaymentScanJob`
- `PaymentVerifyJob`
- `InvoiceExpireJob`

---

## 9. 当前最推荐的开工方式

如果你现在要正式开工，推荐顺序是：

1. 初始化 `backend/` Spring Boot 工程
2. 接入 PostgreSQL、Redis、Flyway
3. 写并跑通基础 migration
4. 做登录、商家固定收款地址配置、Invoice 创建与查询
5. 实现 payment request 生成
6. 实现 Solana 交易扫描与验证
7. 打通第一条完整支付闭环
8. 最后再补 Agent

这套顺序的核心思想是：

- 先把数据库和主链路打稳
- 先把固定地址 + reference 的归因闭环跑通
- Agent 和复杂异步能力放在后面增强

---

## 10. 最终结论

- 你现在可以直接开工
- Flyway 是正确选择，建议继续作为数据库迁移唯一入口
- 当前最值得先做的不是继续写更多总设计，而是按本文档初始化工程和数据库并打通第一条支付验证闭环
- 第一目标不是“把所有模块都写一点”，而是先跑通一条可演示的 `fixed address + reference` 支付链路

一句话总结：

**先用 Spring Boot + PostgreSQL + Redis + Flyway 把固定地址、支付请求、交易验证、核销这条主链路做穿，再在此基础上叠加 Agent 和其他增强能力。**
