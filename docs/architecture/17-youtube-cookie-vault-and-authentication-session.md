# Architecture Note 17: YouTube Cookie Vault and Persistent Authentication Session Flow

## Overview
Security architecture for YouTube account integration. Documents Android Keystore hardware-backed encryption for user cookies, Netscape cookie parser, SAPISIDAUTH authorization header generation, and account profile sync.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
