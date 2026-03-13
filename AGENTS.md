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

## Code Comments

- Add concise method comments for public interface methods and other externally consumed public methods.
- Interface-layer methods should explain the contract briefly, including the purpose of key parameters or special behavior when it is not obvious.
- Keep comments short and high-signal; avoid repeating what is already obvious from the method name.

## Service Layer

- Service definitions should use interface and implementation layering consistently.
- Name the interface as `XxxService` and the implementation as `XxxServiceImpl`.
- Controllers and other services should depend on the service interface instead of the implementation class.
- Put the interface and implementation in the same business module `service` package unless there is a strong reason to split them further.

## Commit Messages

- Use the format `<type>(<module>): <主题>`.
- Keep the `type` compatible with Conventional Commits, such as `feat`, `fix`, `refactor`, `docs`, `test`, or `chore`.
- Prefer a short module scope such as `blockchain`, `invoice`, `merchant`, `auth`, `frontend`, `docs`, or `repo`.
- Prefer Chinese for the subject; English is optional when it improves clarity for reviewers.
- Example: `refactor(blockchain): 重构 Solana RPC 交易解析`
- Optional bilingual example: `refactor(blockchain): 重构 Solana RPC 交易解析 / Refactor Solana RPC transaction parsing`
