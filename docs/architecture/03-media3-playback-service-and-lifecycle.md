# Architecture Note 03: Media3 Playback Service Architecture and Foreground Lifecycle

## Overview
Architecture of AndroidX Media3 ExoPlayer integration. Covers MediaSessionService lifecycle, foreground service notification guarantees, audio focus handling, lock screen controls, and crash-resilient playback state recovery.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
