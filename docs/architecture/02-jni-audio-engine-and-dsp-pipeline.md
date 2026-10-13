# Architecture Note 02: C++ JNI Audio Engine and Real-Time DSP Pipeline

## Overview
Deep architecture dive into the native C++ audio processing bridge (jni_bridge.cpp). Details 32-bit floating-point PCM buffers, real-time parametric biquad equalizer filters, bass boost curves, virtualizer, and sample rate conversion.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
