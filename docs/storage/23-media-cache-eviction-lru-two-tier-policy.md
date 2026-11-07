# Media Cache Eviction Two-Tier LRU and Priority Quota Management

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media cache eviction two-tier lru and priority quota management** within the Nocturne application.

## Key Principles & Invariants
- Segregates local storage into user-pinned offline downloads and ephemeral playback cache.
- Implements strict Least Recently Used (LRU) eviction on ephemeral cache when user quota is reached.
- Never evicts explicitly downloaded offline albums or playlists without direct user deletion action.
- Uses non-blocking background coroutine dispatchers to prune old cache entries during app boot.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
