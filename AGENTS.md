# Repository Conventions

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
- Core entity classes should have a brief class-level comment that explains their responsibility in the business flow.
- Public service interface methods and other externally consumed public methods should have concise contract comments.
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
