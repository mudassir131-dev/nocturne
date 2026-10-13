# Architecture Note 10: Spotify Canvas Video Loop Extraction and SurfaceView Rendering

## Overview
Architecture of the Canvas video looping pipeline. Details backend resolver integration, MP4 video chunk caching, seamless SurfaceView playback synchronization with audio timeline, and low-power battery threshold pausing.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
