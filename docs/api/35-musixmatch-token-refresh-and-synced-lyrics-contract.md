# Musixmatch Token Refresh and Synced Lyrics Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **musixmatch token refresh and synced lyrics contract** within the Nocturne application.

## Key Principles & Invariants
- User token acquisition endpoint: `GET /ws/1.1/token.get` persisting ephemeral user token in encrypted preferences.
- Macro-synced lyrics endpoint: `GET /ws/1.1/track.subtitle.get` with track ISRC or fuzzy title and artist parameters.
- Line-by-line millisecond timestamp parsing with karaoke word-level timing interpolation fallback.
- Error mapping for 401 token expiration triggering immediate transparent token refresh and query retry.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
