# In Memory Artist Metadata LRU Cache Serialization

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **in memory artist metadata lru cache serialization** within the Nocturne application.

## Key Principles & Invariants
- Two-level caching hierarchy combining high-speed memory cache (L1) with Room database persistence (L2).
- LruCache sized dynamically based on available JVM runtime heap memory (maximum 8 MB footprint).
- Fast serialization and deserialization of artist bios, discography listings, and related artist IDs.
- Invalidation listener purging artist cache entries upon manual metadata edit or network refresh.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
