# Unison Room State Sync and Authority Transfer Contract

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **unison room state sync and authority transfer contract** within the Nocturne application.

## Key Principles & Invariants
- Bidirectional WebSocket protocol governing shared listening party room state and participant synchronization.
- Dynamic host authority transfer message schemas handling host disconnects and peer leadership election.
- NTP-style clock synchronization payloads computing network latency and clock offset for sample-accurate play/pause.
- Participant presence heartbeat format with automatic stale peer disconnection after 15 seconds.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.api`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
