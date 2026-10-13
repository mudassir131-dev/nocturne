# ADR-011: Adopt Persistent Offline-First Scrobble Queue for Last.fm

## Status
Accepted

## Context
Scrobbling tracks immediately via HTTP fails when users listen to music offline or in flight mode, losing scrobble history.

## Decision
Store scrobble events in a local SQLite queue and dispatch them in batches via Last.fm API when connectivity returns.

## Consequences
Guarantees zero lost scrobbles regardless of network availability.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
