# Encrypted Database SQLCipher Passphrase Derivation KDF

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **encrypted database sqlcipher passphrase derivation kdf** within the Nocturne application.

## Key Principles & Invariants
- SQLCipher database layer securing song history, playback bookmarks, and synced offline auth tokens at rest.
- Passphrase derivation utilizing Argon2id key derivation function with 64MB memory cost and 4-pass iteration limits.
- Hardware-backed master secret retrieved from Android Keystore acting as the root entropy source for salt generation.
- Automated memory cleanup and explicit SQLite cipher key clearing on activity lifecycle background pauses.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
