# Audio Resampling and Format Conversion Pipeline Stages

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audio resampling and format conversion pipeline stages** within the Nocturne application.

## Key Principles & Invariants
- Multi-stage pipeline transforming variable input PCM formats (16-bit, 24-bit, 32-bit float) to target output bit depth.
- High-order polyphase sinc resampler delivering anti-aliasing filtering during sample rate conversion.
- Interleaved to non-interleaved planar buffer conversion enabling SIMD vector processing across DSP stages.
- Zero-copy buffer recycling minimizing Android garbage collection pauses in high-throughput audio threads.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
