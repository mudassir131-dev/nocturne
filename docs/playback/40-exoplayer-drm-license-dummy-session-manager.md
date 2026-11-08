# ExoPlayer DRM License Dummy Session Manager

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exoplayer drm license dummy session manager** within the Nocturne application.

## Key Principles & Invariants
- Stub DRM session manager bypassing Widevine license acquisition overhead for DRM-free InnerTube audio streams.
- Eliminates security framework initialization latency (up to 300ms reduction in initial playback startup time).
- Transparently routes clear audio chunks directly to media demuxers without crypto context allocation.
- Guard condition ensuring graceful fallback to standard MediaDrmCallback should encrypted media items be queued.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
