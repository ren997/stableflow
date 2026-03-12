# StableFlow

Stablecoin payment workflow infrastructure, first built on Solana.

## Repository layout

- `docs/`: product, technical, and implementation documents
- `backend/`: Spring Boot backend
- `frontend/`: React frontend

## Overview
StableFlow is a workflow-driven stablecoin billing, payment, and reconciliation system for global merchants.

## Commit message linting

This repository uses `commitlint` with the Conventional Commits preset.

### Local setup

```bash
npm install
```

After installation, Husky will register the `commit-msg` hook automatically.

### Commit message examples

```text
feat(invoice): 新增账单创建流程 / Add invoice creation flow
docs(repo): 完善 StableFlow 需求文档 / Refine StableFlow requirements
refactor(blockchain): 重构 Solana RPC 交易解析 / Refactor Solana RPC transaction parsing
```

### Recommended format

Use the format `<type>(<module>): <中文主题> / <English subject>`.

### CI enforcement

GitHub Actions validates commit messages for pull requests in:

`/.github/workflows/commitlint.yml`
