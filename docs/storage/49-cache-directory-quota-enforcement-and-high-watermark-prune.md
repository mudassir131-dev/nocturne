# Cache Directory Quota Enforcement and High Watermark Prune

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **cache directory quota enforcement and high watermark prune** within the Nocturne application.

## Key Principles & Invariants
- User-configurable cache disk space limit (500MB to 20GB) with high watermark eviction trigger at 90% capacity.
- Least Recently Used (LRU) eviction algorithm purging cached streaming chunks and unpinned audio data.
- Exclusion list protecting pinned offline downloads and favorite song files from cache eviction cycles.
- Background disk usage scanner reporting exact storage breakdown (Audio, Artwork, Lyrics, Database) in Settings.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
