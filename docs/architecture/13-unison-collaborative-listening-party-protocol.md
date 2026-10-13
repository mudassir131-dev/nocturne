# Architecture Note 13: Unison Collaborative Listening Party Protocol and WebSocket State Sync

## Overview
Architecture of Unison peer-to-peer and relayed listening sessions. Documents WebSocket message framing, room join/leave protocols, host authoritative playback control, clock drift compensation, and track queue synchronization.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
