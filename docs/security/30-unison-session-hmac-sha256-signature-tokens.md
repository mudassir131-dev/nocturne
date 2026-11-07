# Unison Collaborative Session HMAC-SHA256 Token Signature Check

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **unison collaborative session hmac-sha256 token signature check** within the Nocturne application.

## Key Principles & Invariants
- Signs all collaborative Unison control messages with HMAC-SHA256 using room shared secret.
- Appends timestamp and monotonic sequence nonce to prevent packet replay and tampering attacks.
- Rejects messages with clock skew exceeding 5 seconds or invalid cryptographic signature.
- Automatically expels untrusted or malformed peer connections from active listening group.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.security`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
