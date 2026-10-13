# Architecture Note 19: Backup, Restore, and Encrypted Cross-Device Migration Schema

## Overview
Data mobility architecture. Details full database export to compressed JSON (.nocturne), AES-256-GCM backup encryption, schema version migration, and idempotent import reconciliation into fresh app installations.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
