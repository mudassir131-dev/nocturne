# MediaCodec Async Mode and Direct Buffer Pipeline

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media codec async mode and direct buffer pipeline** within the Nocturne application.

## Key Principles & Invariants
- Asynchronous MediaCodec pipeline utilizing MediaCodec.Callback for input and output buffer processing.
- Direct native ByteBuffer memory allocations avoiding memory copies between Java heap and hardware decoders.
- Dedicated decoding HandlerThread isolated from both the UI main thread and the primary audio playback thread.
- Seamless format change handling when transitioning between different audio bit depths (16-bit vs 24-bit PCM).

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
