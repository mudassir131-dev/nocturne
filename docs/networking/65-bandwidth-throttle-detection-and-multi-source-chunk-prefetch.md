# Bandwidth Throttle Detection and Multi Source Chunk Prefetch

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **bandwidth throttle detection and multi source chunk prefetch** within the Nocturne application.

## Key Principles & Invariants
- Chunk download rate slope monitoring detecting carrier-level video/audio throttling algorithms.
- Parallel byte-range HTTP request splitting (1 MB chunks) distributed across multiple concurrent TCP connections.
- Dynamic chunk assembly and sequence reordering ensuring contiguous stream delivery into ExoPlayer source.
- Automatic TCP window size probing to saturate high-latency long-fat networks (LFN) on mobile data.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
