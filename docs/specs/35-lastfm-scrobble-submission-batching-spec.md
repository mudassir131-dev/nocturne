# Last.fm Scrobble API 2.0 Batch Submission and Timestamp Rules Spec

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **last.fm scrobble api 2.0 batch submission and timestamp rules spec** within the Nocturne application.

## Key Principles & Invariants
- Batch payload limit: Up to 50 scrobbles per single HTTP POST request to `track.scrobble`.
- MD5 signature generation concatenating method, api_key, session_key, and alphabetized params.
- Timestamp validation: UNIX epoch timestamp indicating the exact moment track playback commenced.
- Handling of API error codes: 9 (Invalid Session Key), 11 (Service Offline), 16 (Service Temporarily Unavailable).

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
