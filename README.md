# StableFlow

Stablecoin payment workflow infrastructure, first built on Solana.

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
feat: add invoice creation flow
docs: refine stableflow requirements
chore: set up commitlint and husky
```

### CI enforcement

GitHub Actions validates commit messages for pull requests in:

`/.github/workflows/commitlint.yml`

### PR validation test

This note exists to trigger a pull request check on the `test` branch.