# Security & Privacy Policy 01: Android Keystore Master Key Generation and Rotation Policy

## Scope
Governs encryption keys used to secure user credentials, YouTube session cookies, and private backup archives.

## Specification
- **Algorithm**: AES-256-GCM authenticated encryption.
- **Provider**: `AndroidKeyStore`.
- **Key Properties**: `KeyGenParameterSpec.Builder` with `PURPOSE_ENCRYPT | PURPOSE_DECRYPT` and `BLOCK_MODE_GCM`.
- **Hardware Backing**: Enforce StrongBox or TEE (Trusted Execution Environment) where available.\n