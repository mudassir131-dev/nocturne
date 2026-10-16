# Security 15: Deep Link & Intent Filter Parameter Sanitization Rules

## Verification
- Deep links (`nocturne://...`) strictly validated against schema whitelist.
- Query parameters sanitized to prevent intent injection or unauthorized navigation redirection.
