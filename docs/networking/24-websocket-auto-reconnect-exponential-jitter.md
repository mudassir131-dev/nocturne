# WebSocket Auto-Reconnect State Machine and Exponential Jitter

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **websocket auto-reconnect state machine and exponential jitter** within the Nocturne application.

## Key Principles & Invariants
- Maintains persistent bidirectional WebSocket channel for Unison collaborative listening sessions.
- Employs Decorrelated Jitter backoff algorithm for resilient reconnect timing.
- Flushes queued unsent client actions once reconnection is successfully re-established.
- Monitors transport-level Ping/Pong frames every 15 seconds to detect half-open socket states.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.network`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
