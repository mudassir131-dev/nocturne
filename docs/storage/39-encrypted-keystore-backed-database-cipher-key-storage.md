# Encrypted Keystore Backed Database Cipher Key Storage

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **encrypted keystore backed database cipher key storage** within the Nocturne application.

## Key Principles & Invariants
- Generates 256-bit AES-GCM master encryption key securely held within hardware Android KeyStore (TEE / StrongBox).
- Database passphrase derived via PBKDF2 with 100,000 iterations and per-installation random salt.
- SQLCipher integration providing page-level encryption for sensitive user cookies and listening history database.
- Prevents data extraction via ADB backup or rooted filesystem inspection on compromised Android devices.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
