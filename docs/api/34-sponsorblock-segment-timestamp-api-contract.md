# SponsorBlock Segment Timestamp API Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sponsorblock segment timestamp api contract** within the Nocturne application.

## Key Principles & Invariants
- Segment query endpoint: `GET https://sponsor.ajay.app/api/skipSegments` with categories music_offtopic, sponsor, intro, and outro.
- Returns millisecond intervals `[start, end]` representing non-music filler, intros, or sponsor sections.
- High-performance binary search lookup during playback tick to trigger seamless auto-skip or visual segment highlighting.
- Local LRU caching of segment arrays keyed by YouTube video ID with a 72-hour TTL.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
