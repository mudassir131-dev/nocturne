# ExoPlayer Progressive Media Source Read Throttling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exo player progressive media source read throttling** within the Nocturne application.

## Key Principles & Invariants
- OkHttp upstream read pacing matching playback bitrate consumption to prevent buffer over-allocation.
- Bounded memory footprint limiting in-memory media chunk cache to 32MB during progressive streaming.
- Dynamic bandwidth throttling reducing background network contention during concurrent album art fetching.
- Automatic read resumption upon buffer depletion below high watermark safety threshold.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
