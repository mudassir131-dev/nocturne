# Unison Collaborative Session NTP Clock Sync and Heartbeat Ping Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **unison collaborative session ntp clock sync and heartbeat ping contract** within the Nocturne application.

## Key Principles & Invariants
- WebSocket message schema: `{ type: 'PING', clientTime: Long }` and `{ type: 'PONG', clientTime: Long, serverTime: Long }`.
- Calculation of network latency: RTT = (t3 - t0) - (t2 - t1), Offset = ((t1 - t0) + (t2 - t3)) / 2.
- Heartbeat cadence: 5-second interval detecting dead peer connections.
- Automatic reconnection with backoff on sudden socket termination.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
