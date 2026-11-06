# Last.fm Scrobbler Persistent Retry Queue and Deduplication Cache

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **last.fm scrobbler persistent retry queue and deduplication cache** within the Nocturne application.

## Key Principles & Invariants
- Offline-first scrobble queue persisted in SQLite database tables.
- 50% playback duration or 4-minute listen threshold enforcement according to Audioscrobbler 2.0 rules.
- Batch submission worker triggering on network reconnection via WorkManager.
- Deduplication hash generation preventing duplicate scrobbles during flaky network transitions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
