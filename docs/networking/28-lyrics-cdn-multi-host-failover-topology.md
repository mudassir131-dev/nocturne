# Lyrics CDN Multi-Host Round-Robin and Failover Topology

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics cdn multi-host round-robin and failover topology** within the Nocturne application.

## Key Principles & Invariants
- Orchestrates lyrics querying across LRCLIB, Paxsenix, and Kugou endpoints in prioritized order.
- Instantly switches to secondary provider if primary returns 404 Not Found or 5xx Server Error.
- Aggregates and normalizes responses into unified SyncedLyrics format regardless of provider syntax.
- Implements client-side rate limiting to stay within third-party free API usage quotas.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.network`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
