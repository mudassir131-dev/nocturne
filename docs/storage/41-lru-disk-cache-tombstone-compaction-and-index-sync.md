# LRU Disk Cache Tombstone Compaction and Index Sync

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lru disk cache tombstone compaction and index sync** within the Nocturne application.

## Key Principles & Invariants
- Custom disk cache journal format recording CLEAN, DIRTY, REMOVE, and READ entry operations.
- Automatic compaction when journal entries exceed 2000 lines, rebuilding snapshot journal and pruning deleted file handles.
- Thread-safe ReadWriteLock synchronizing disk cache index across foreground player and background pre-fetch workers.
- Recovers automatically from power loss or abrupt process termination by rolling back uncommitted dirty entries.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
