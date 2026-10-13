# Architecture Note 18: Lossless Audio Decoding and 24-Bit / 96kHz Hi-Res Playback

## Overview
High-resolution audio playback architecture. Explains bit-perfect audio routing, Android AudioTrack direct output flag negotiation, FLAC/ALAC decoder pipelines, and dynamic sample rate switching for external USB DACs.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
