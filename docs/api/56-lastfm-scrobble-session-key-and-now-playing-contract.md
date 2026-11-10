# Last.fm Scrobble Session Key and Now Playing Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lastfm scrobble session key and now playing contract** within the Nocturne application.

## Key Principles & Invariants
- Specification for Last.fm API 2.0 auth handshake exchanging web token for persistent mobile session key (sk).
- Request signing protocol computing MD5 API signatures over sorted UTF-8 parameter lists and shared API secrets.
- Implementation contract for track.updateNowPlaying dispatched within 5 seconds of track start.
- Implementation contract for track.scrobble batch uploads containing timestamp, track, artist, and duration metadata.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
