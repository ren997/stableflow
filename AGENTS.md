# Repository Conventions

## Naming

- Request/input parameter objects must use the `Dto` suffix.
- Response/output view objects must use the `Vo` suffix.
- When adding new Java classes, prefer `Dto` for inbound data and `Vo` for outbound data consistently across modules.

## API Modeling

- Do not expose raw `JsonNode` or generic JSON structures as outward-facing return models when the structure is known.
- Prefer explicit Java entities for request and response models.
- If a response contains nested structures, prefer static inner classes inside the outer `Vo`.
- Add concise field comments for request and response model fields.

## Commit Messages

- Use the format `<type>(<module>): <主题>`.
- Keep the `type` compatible with Conventional Commits, such as `feat`, `fix`, `refactor`, `docs`, `test`, or `chore`.
- Prefer a short module scope such as `blockchain`, `invoice`, `merchant`, `auth`, `frontend`, `docs`, or `repo`.
- Prefer Chinese for the subject; English is optional when it improves clarity for reviewers.
- Example: `refactor(blockchain): 重构 Solana RPC 交易解析`
- Optional bilingual example: `refactor(blockchain): 重构 Solana RPC 交易解析 / Refactor Solana RPC transaction parsing`
