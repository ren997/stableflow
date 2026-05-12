# StableFlow

[中文说明](./README.zh-CN.md) | [English](./README.md)

> 2026 年 4 月 Solana 黑客松参赛作品

StableFlow 是一个面向出海数字商家的 Solana 原生稳定币账单、支付验证与核销基础设施。

它希望把开单、收款、链上验证、自动核销和支付证明生成整合进一条清晰工作流里，帮助商户更低成本地使用稳定币完成全球数字收款。

## 项目简介

在跨境数字收款场景中，商家常常需要在报价、开单、催款、收款确认和对账之间切换多个系统。StableFlow 希望把这些环节收拢到一个产品里，并围绕 Solana 上的 USDC 收款提供一套可落地的标准流程。

当前 MVP 聚焦一条清晰主线：

1. 商户登录系统
2. 配置固定 Solana 收款地址
3. 创建 Invoice
4. 为账单生成唯一 Reference 和支付请求
5. 客户打开公共支付页并完成付款
6. 后台扫描链上交易并验证支付
7. 系统自动核销账单并生成 Payment Proof
8. 商户在 Dashboard 查看状态和汇总结果

当前核心归因策略是：

**固定收款地址 + Reference**

这让 StableFlow 在黑客松和 MVP 阶段可以更稳定地完成订单归因、链上验证和账单核销闭环。

## 解决什么问题

- 稳定币收款流程分散，人工确认成本高
- 商户缺少围绕 Invoice 的标准化支付验证流程
- 链上到账后，订单归因和对账仍然依赖手工处理
- 商户和客户都需要可追踪的支付结果与支付证明

StableFlow 的目标不是做钱包或交易所，而是做一层面向商户的支付工作流基础设施。

## 核心能力

- 商户登录与基础账单管理
- 固定 Solana 收款地址配置
- Invoice 创建、激活与公开支付页生成
- Solana 原生支付信息展示，包括金额、收款地址、Reference 和 Mint
- 链上交易扫描与支付验证
- 自动核销与支付状态更新
- Payment Proof 生成与展示
- Dashboard 汇总已支付、待支付和异常账单
- Agent 能力作为增强层，建立在已验证的账单与支付事实之上

## Demo

- 在线演示地址：[http://110.40.155.140/](http://110.40.155.140/)
- 演示操作说明：[docs/demo-operation-guide.md](docs/demo-operation-guide.md)
- 部署说明：[docs/deployment.md](docs/deployment.md)
- 公共支付页示例：[http://110.40.155.140/pay/pub_d4b4579f5e304459b9273cca0320ef0f](http://110.40.155.140/pay/pub_d4b4579f5e304459b9273cca0320ef0f)
- 演示网络：Solana Devnet

如果你想快速理解项目，建议直接走一遍演示主流程：登录后台、配置收款地址、创建账单、激活账单、打开公共支付页、观察支付状态变化。

## 演示截图

### Dashboard

![StableFlow Dashboard](docs/images/demo/02-dashboard.png)

### Invoice 管理

![StableFlow Invoices](docs/images/demo/07-invoice-activated.png)

### 公共支付页

![StableFlow Public Payment Page](docs/images/demo/08-public-payment-page.png)

## 产品流程

```text
商户配置固定收款地址
  -> 创建账单
  -> 生成唯一 Reference 和支付请求
  -> 客户打开公共支付页
  -> 客户在 Solana 上发送 USDC
  -> 后台扫描链上交易
  -> 校验收款地址 / Reference / 币种 / 金额 / 时间窗口
  -> 核销账单并生成支付证明
  -> 在 Dashboard 更新商户侧支付状态
```

## 技术栈

- Backend: Spring Boot 3, Java 21, MyBatis-Plus, Flyway
- Frontend: React 18, Vite, TypeScript, Ant Design, React Query
- Data: PostgreSQL, Redis
- Blockchain: Solana RPC, sol4k
- Deployment: Docker Compose, Nginx

## 仓库结构

- `docs/`: 产品、架构、实现和部署文档
- `backend/`: Spring Boot 后端
- `frontend/`: React 前端

## 快速开始

如果你想部署当前版本，推荐先看：

- [docs/deployment.md](docs/deployment.md)

当前推荐方式是单机 Docker Compose 部署，前后端同机运行，PostgreSQL、Redis 和 Solana RPC 使用外部实例。

## 进一步阅读

- [docs/requirements.md](docs/requirements.md)
- [docs/technical-design.md](docs/technical-design.md)
- [docs/implementation-guide.md](docs/implementation-guide.md)
- [docs/dev-tasks.md](docs/dev-tasks.md)

## 项目定位

StableFlow 不是钱包，不是交易所，也不是单纯的聊天机器人。

它更像一层面向商户的稳定币支付工作流基础设施，用来把以下能力串起来：

- 开单
- 付款请求生成
- 链上支付识别
- 订单归因
- 自动核销
- 支付证明
- 基础运营与对账汇总
