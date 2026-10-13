# ADR-014: Adopt Android Keystore Encrypted Preferences for YouTube Cookies

## Status
Accepted

## Context
Storing sensitive YouTube authentication cookies in plaintext XML files exposed user sessions to security risks on rooted devices.

## Decision
Encrypt all cookie strings using AndroidX Security EncryptedSharedPreferences backed by hardware Keystore AES-256 keys.

## Consequences
Provides enterprise-grade protection for sensitive user credentials on the device.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
