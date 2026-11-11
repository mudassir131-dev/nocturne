# Media Chunk Atomic Rename and Corrupted Partial Cleanup

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media chunk atomic rename and corrupted partial cleanup** within the Nocturne application.

## Key Principles & Invariants
- Atomic rename pattern writing incoming audio chunks to .tmp extension before moving to permanent cache hash.
- Startup scanning routine identifying and purging abandoned .tmp partial downloads caused by process crashes.
- SHA-256 content checksum verification against server HTTP ETag / digest before finalizing file commit.
- Invariant guaranteeing ExoPlayer cache index never points to corrupted or half-written audio fragments.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
