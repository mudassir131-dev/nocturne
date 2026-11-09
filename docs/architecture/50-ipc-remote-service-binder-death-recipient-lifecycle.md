# IPC Remote Service Binder Death Recipient Lifecycle

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **ipc remote service binder death recipient lifecycle** within the Nocturne application.

## Key Principles & Invariants
- IBinder.DeathRecipient registration monitoring background music playback service death and crash events.
- Clean client-side state reset and automatic reconnect attempts with exponential backoff on unexpected service termination.
- Persistent token and state restoration restoring playing song, position, and queue upon service recreation.
- Thread-safe synchronization preventing deadlocks during concurrent bind and unbind invocations.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.architecture`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
