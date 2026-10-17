# Storage 19: LRC and TTML Synchronized Lyrics LRU Persistence Schema

## Lyrics Storage
- Dedicated SQLite table stores parsed syllable-level timestamped lyrics keyed by track hash.
- Implements 100MB LRU eviction tier ensuring instant lyric rendering in offline airplane mode.

