# SponsorBlock Skip Category Filtering and Voting Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sponsorblock skip category filtering and voting contract** within the Nocturne application.

## Key Principles & Invariants
- API endpoint GET /api/skipSegments query filtering by categories: music_offtopic, sponsor, intro, and outro.
- Segment voting contract POST /api/voteOnSponsorTime enabling community upvoting or downvoting of segment accuracy.
- Local UUID generation for anonymized submission hash routing preserving user privacy.
- In-memory segment tree structure delivering O(log n) lookups during continuous playback position updates.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
