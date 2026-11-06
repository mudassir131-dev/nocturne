# Lock-Free Single-Producer Single-Consumer Audio Ring Buffer

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lock-free single-producer single-consumer audio ring buffer** within the Nocturne application.

## Key Principles & Invariants
- Memory model fences and atomic head/tail pointers preventing thread contention.
- Zero mutex contention on the real-time audio playback rendering thread.
- Bounded memory footprint with cache-line padded data structures to prevent false sharing.
- High-throughput PCM sample transfers between network decoder and AudioTrack sink.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
