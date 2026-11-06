# LrcLib Synced and Plain Lyrics Search GET Request Query Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lrclib synced and plain lyrics search get request query contract** within the Nocturne application.

## Key Principles & Invariants
- Endpoint: `GET https://lrclib.net/api/get?artist_name={artist}&track_name={track}&duration={duration}`.
- Fallback endpoint: `GET https://lrclib.net/api/search?q={query}`.
- Response fields: `syncedLyrics` (LRC format timestamped) and `plainLyrics` (text only).
- Fast in-memory caching and disk persistence in Room lyrics database.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
