# ExoPlayer LoadControl Dynamic Buffer Duration Tuning

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exoplayer loadcontrol dynamic buffer duration tuning** within the Nocturne application.

## Key Principles & Invariants
- Configures DefaultLoadControl with minBufferMs of 15,000 ms and maxBufferMs of 50,000 ms.
- Prioritizes bufferForPlaybackMs at 1,500 ms for near-instant playback initiation.
- Dynamically scales buffer boundaries based on active network connection type (Wi-Fi vs Cellular).
- Prevents buffer thrashing during high-bitrate lossless stream preloading.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
