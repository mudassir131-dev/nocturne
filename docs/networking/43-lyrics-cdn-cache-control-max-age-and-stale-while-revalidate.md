# Lyrics CDN Cache Control Max Age and Stale While Revalidate

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **lyrics cdn cache control max age and stale while revalidate** within the Nocturne application.

## Key Principles & Invariants
- Injects Cache-Control: max-age=604800, stale-while-revalidate=86400 headers on lyrics API requests.
- Serves cached LRC lyrics instantly to UI while background coroutine validates and updates updated timestamp lines.
- Eliminates lyrics screen loading spinners for previously played songs.
- Hash-based ETag validation against Kugou and Lrclib servers minimizing redundant payload transfer.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.networking`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
