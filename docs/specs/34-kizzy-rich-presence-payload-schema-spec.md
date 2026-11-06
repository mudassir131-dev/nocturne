# Discord Rich Presence IPC Activity Payload JSON Schema Specification

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **discord rich presence ipc activity payload json schema specification** within the Nocturne application.

## Key Principles & Invariants
- JSON schema defining activity details, state, timestamps (start, end), and assets (large_image, small_image).
- Validation of field length constraints: maximum 128 characters for title and artist strings.
- Image asset URL resolution through high-resolution Nocturne proxy endpoints.
- Heartbeat cadence: Broadcast intent refresh dispatched every 15 seconds during active playback.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
