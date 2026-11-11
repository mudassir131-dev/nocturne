# Hardware Backed Keystore RSA and ECDSA Key Generation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **hardware backed keystore rsa and ecdsa key generation** within the Nocturne application.

## Key Principles & Invariants
- KeyPairGenerator utilization targeting Android KeyStore with StrongBox or TEE hardware security module.
- ECDSA P-256 and RSA 4096-bit asymmetric key pair generation for local authentication and data signing.
- Flag KeyProperties.PURPOSE_ENCRYPT combined with REQUIRE_USER_PRESENCE enforcing hardware gating.
- Cryptographic verification that private key material never enters RAM and cannot be exported.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
