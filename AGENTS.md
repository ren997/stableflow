# Repository Conventions

## Project Overview

- StableFlow is a Solana stablecoin billing, payment verification, and reconciliation infrastructure project for global digital merchants.
- The MVP core flow is: merchant login -> configure fixed receiving address -> create invoice -> generate payment request -> scan on-chain transactions -> verify payment -> reconcile invoice -> generate payment proof -> show dashboard summary.
- The current product strategy is `fixed address + reference`. Do not redesign the main attribution model unless the user explicitly asks for it.
- Agent capability is an enhancement layer. It should be built on top of verified invoice and payment facts, not used as the source of payment truth.

## Repository Layout

- `docs/`: product, architecture, implementation, and execution documents. This is the main context source for new threads.
- `backend/`: Spring Boot backend, currently the main implementation area.
- `frontend/`: React + Vite frontend, currently still relatively early and should follow backend capability delivery.

## How To Read The Docs

When starting a new thread, read the docs in this order unless the user asks for something very specific:

1. `docs/requirements.md`
   Understand product goals, MVP scope, core user scenarios, and business rules.
2. `docs/technical-design.md`
   Understand architecture, module boundaries, payment flow, verification rules, and data model.
3. `docs/implementation-guide.md`
   Understand the recommended package structure, migration strategy, API skeleton, and delivery approach.
4. `docs/dev-tasks.md`
   Use this as the execution board for what is done, what is pending, and what should be built next.

## What `docs/dev-tasks.md` Means

- `docs/dev-tasks.md` is the main development task board for the MVP.
- It translates product/design documents into executable milestones such as `M0` to `M7` and concrete tasks such as `T101`, `T203`, `T404`.
- Each task includes status, dependency, deliverables, and completion criteria.
- When the user asks "what should we do next", prefer checking `docs/dev-tasks.md` first instead of guessing from README text.
- Treat task status in `docs/dev-tasks.md` as the primary planning reference, and then verify against actual code when needed.

## New Thread Quick Start

For a fresh thread, use this workflow:

1. Read `docs/dev-tasks.md` to locate the current stage and pending tasks.
2. Read the related section in `docs/technical-design.md` for the business flow and design constraints.
3. Open the corresponding backend or frontend module to verify whether the task is already implemented.
4. Prefer continuing the current MVP mainline before jumping to P1 or speculative refactors.

## Current Delivery Heuristics

- Prioritize the MVP payment closure over extensions: payment proof, payment status, dashboard, public payment page, expiration handling, and job stability are typically more important than Agent features at this stage.
- Prefer backend completion of the main payment workflow before doing broad frontend polish.
- If README files conflict with code or `docs/dev-tasks.md`, trust the code and `docs/dev-tasks.md` first because some README notes may lag behind implementation progress.

## Naming

- Request/input parameter objects must use the `Dto` suffix.
- Response/output view objects must use the `Vo` suffix.
- Enum types must use the `Enum` suffix.
- Business enum types should provide clear semantics such as `code` and `desc`, and expose getters when the enum is used in persistence, API payloads, or status mapping.
- When adding new Java classes, prefer `Dto` for inbound data and `Vo` for outbound data consistently across modules.

## API Modeling

- Do not expose raw `JsonNode` or generic JSON structures as outward-facing return models when the structure is known.
- Prefer explicit Java entities for request and response models.
- If a response contains nested structures, prefer static inner classes inside the outer `Vo`.

## Comment Conventions

- Request, response, and entity fields should have concise field comments.
- Record classes should have brief class-level comments, and record components should have concise field comments.
- Core entity classes should have a brief class-level comment that explains their responsibility in the business flow.
- Public service interface methods and other externally consumed public methods should have concise contract comments.
- Business orchestration code should have brief process comments at key steps so readers can quickly understand the main flow.
- Scheduled job classes and scheduled methods should have brief comments explaining the trigger purpose and business responsibility.
- Interface-layer comments should explain the contract briefly, including key parameters or special behavior when it is not obvious.
- Enum classes should have brief class-level comments, and enum items should carry readable descriptions when they represent business states or externally visible codes.
- Keep comments short and high-signal; avoid repeating what is already obvious from the method or field name.

## Service Layer

- Service definitions should use interface and implementation layering consistently.
- Name the interface as `XxxService` and the implementation as `XxxServiceImpl`.
- Controllers and other services should depend on the service interface instead of the implementation class.
- Put the interface and implementation in the same business module `service` package unless there is a strong reason to split them further.
- For CRUD-oriented MyBatis-Plus services, prefer `XxxService extends IService<Entity>` and `XxxServiceImpl extends ServiceImpl<Mapper, Entity>`.
- Do not force `IService` onto orchestration-style services that are not centered on a single entity aggregate.

## Commit Messages

- Use the format `<type>(<module>): <主题>`.
- Keep the `type` compatible with Conventional Commits, such as `feat`, `fix`, `refactor`, `docs`, `test`, or `chore`.
- Prefer a short module scope such as `blockchain`, `invoice`, `merchant`, `auth`, `frontend`, `docs`, or `repo`.
- Prefer Chinese for the subject; English is optional when it improves clarity for reviewers.
- Example: `refactor(blockchain): 重构 Solana RPC 交易解析`
- Optional bilingual example: `refactor(blockchain): 重构 Solana RPC 交易解析 / Refactor Solana RPC transaction parsing`
