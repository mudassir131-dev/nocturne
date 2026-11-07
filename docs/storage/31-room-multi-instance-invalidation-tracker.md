# Room Multi-Instance Invalidation Tracker Across Process Boundaries

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **room multi-instance invalidation tracker across process boundaries** within the Nocturne application.

## Key Principles & Invariants
- Configures Room MultiInstanceInvalidationService to synchronize DB changes between UI and playback service.
- Automatically notifies UI observers (StateFlow / Flow) when playback service records track history.
- Eliminates stale UI states without requiring manual polling or IPC broadcast intents.
- Cleanly unregisters invalidation listeners on service shutdown.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.db`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
