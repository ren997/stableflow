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

## Service conventions

Use interface and implementation layering for services:

- `XxxService` for the service interface
- `XxxServiceImpl` for the Spring-managed implementation
- Controllers and peer services should depend on the interface
- CRUD-oriented MyBatis-Plus services should prefer `IService<Entity>` and `ServiceImpl<Mapper, Entity>`

## Comment conventions

- Request, response, and entity fields should have concise field comments
- Core entity classes should have brief class-level responsibility comments
- Public service interface methods should have concise contract comments
- Scheduled job classes and scheduled methods should have brief purpose comments

### Local setup

```bash
npm install
```

After installation, Husky will register the `commit-msg` hook automatically.

### Commit message examples

```text
feat(invoice): 新增账单创建流程
docs(repo): 完善 StableFlow 需求文档
refactor(blockchain): 重构 Solana RPC 交易解析
```

### Recommended format

Use the format `<type>(<module>): <subject>`.

Chinese is preferred for day-to-day development. English is optional when it helps external reviewers.

### CI enforcement

GitHub Actions validates commit messages for pull requests in:

`/.github/workflows/commitlint.yml`
