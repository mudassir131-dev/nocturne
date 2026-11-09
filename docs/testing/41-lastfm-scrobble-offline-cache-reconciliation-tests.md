# Last.fm Scrobble Offline Cache Reconciliation Tests

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lastfm scrobble offline cache reconciliation tests** within the Nocturne application.

## Key Principles & Invariants
- Offline playback simulation queueing 200 consecutive song scrobbles without network connectivity.
- Reconnection test suite verifying correct chronological scrobble submission with original listen timestamps.
- Rate limit response simulation (HTTP 429) verifying exponential backoff and persistent database retention.
- Duplicate prevention assertions verifying idempotent submission keys across retry attempts.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.testing`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
