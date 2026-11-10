# Piped and Invidious Fallback Proxy REST Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **piped and invidious fallback proxy rest contract** within the Nocturne application.

## Key Principles & Invariants
- REST API contract for querying public and self-hosted Piped and Invidious instances as secondary stream extractors.
- Audio stream array deserialization filtering for high-bitrate Opus (audio/webm) and AAC (audio/mp4) stream formats.
- Instance health check probe verifying API latency, CORS status, and rate limit headroom before dispatching extraction queries.
- Fallback triggering logic invoked automatically upon receiving persistent YouTube HTTP 403 or signature cipher decryption errors.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
