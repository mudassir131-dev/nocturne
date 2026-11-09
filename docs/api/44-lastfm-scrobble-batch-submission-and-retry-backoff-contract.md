# Last.fm Scrobble Batch Submission and Retry Backoff Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lastfm scrobble batch submission and retry backoff contract** within the Nocturne application.

## Key Principles & Invariants
- Batch scrobble endpoint track.scrobble supporting up to 50 queued track items per atomic HTTP POST payload.
- MD5 signature generation across sorted key-value pairs utilizing application secret and session key credentials.
- Exponential backoff with jitter on HTTP 5xx responses and connection timeouts preventing network flooding.
- Deduplication hash table preventing double-scrobbling on unexpected app termination or network reconnection.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
