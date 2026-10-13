# ADR-008: Adopt OkHttp Disk Cache with 2GB LRU Eviction for Audio Chunks

## Status
Accepted

## Context
Re-fetching audio streams on repeat listens consumed excessive mobile data and caused playback latency on weak cellular networks.

## Decision
Inject an OkHttp / ExoPlayer SimpleCache disk cache bounded at 2GB with LRU chunk eviction.

## Consequences
Enables instant offline repeat playback for recently played tracks and saves gigabytes of user cellular bandwidth.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
