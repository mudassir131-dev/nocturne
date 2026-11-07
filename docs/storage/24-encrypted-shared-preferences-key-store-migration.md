# EncryptedSharedPreferences Keystore Master Key Rotation Flow

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **encryptedsharedpreferences keystore master key rotation flow** within the Nocturne application.

## Key Principles & Invariants
- Employs MasterKey with AES256_GCM scheme for securing user settings, auth cookies, and addon tokens.
- Gracefully handles Android Keystore invalidation caused by lock-screen credential changes.
- Provides automatic recovery bridge: recreates preferences file safely without crashing the app.
- Validates stored keys with HMAC integrity checks before decrypting payload values.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
