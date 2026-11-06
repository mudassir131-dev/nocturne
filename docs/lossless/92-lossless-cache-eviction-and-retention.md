# Least-Recently-Used Cache Eviction Policy for Lossless Audio Files

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **least-recently-used cache eviction policy for lossless audio files** within the Nocturne application.

## Key Principles & Invariants
- Dual-tier LRU cache tracking both access frequency and byte size of cached lossless tracks.
- Preservation of pinned playlists and user-favorited high-resolution albums.
- Background disk space garbage collection scheduled via WorkManager during idle charging.
- Configurable storage quotas ranging from 1GB to 64GB with accurate disk usage reporting.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
