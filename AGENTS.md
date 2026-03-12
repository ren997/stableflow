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
