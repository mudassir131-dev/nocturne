# ADR-019: Adopt Duration Delta and Artist Tokenization for Track Deduplication

## Status
Accepted

## Context
Playlists frequently contain identical tracks with slightly varied upload titles ("Official Video", "Audio", "Lyrics").

## Decision
Identify duplicates when artist token overlap is >= 80% and track duration matches within a ±3 second tolerance window.

## Consequences
Accurately prevents duplicate tracks from clogging user queues and playlist imports.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
