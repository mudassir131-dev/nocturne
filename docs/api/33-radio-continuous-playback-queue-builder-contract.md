# Radio Continuous Playback Queue Builder Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **radio continuous playback queue builder contract** within the Nocturne application.

## Key Principles & Invariants
- Auto-mix radio seed endpoint: `POST /youtubei/v1/next` with `playlistId: RDAMVM<videoId>` generating infinite track queues.
- Continuation token caching ensuring seamless pre-fetching of next 25 radio recommendations before queue underrun.
- Deduplication filter filtering out songs played within the last 50 tracks in the current listening session.
- Fallback artist radio generation if video seed radio returns fewer than 5 candidate tracks.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
