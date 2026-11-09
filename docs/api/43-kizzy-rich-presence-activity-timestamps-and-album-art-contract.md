# Kizzy Rich Presence Activity Timestamps and Album Art Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **kizzy rich presence activity timestamps and album art contract** within the Nocturne application.

## Key Principles & Invariants
- Kizzy IPC broadcast intent com.my.kizzy.RPC transmitting song title, artist, duration, and elapsed playback progress.
- Real-time Unix epoch start and end timestamp calculations preserving Discord presence synchronization across track seeking.
- External CDN cover art proxy URL resolution matching Discord asset dimensions (512x512 max).
- Debounced intent dispatcher throttling update frequency to under 1 event per 4 seconds to comply with Discord rate limits.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
