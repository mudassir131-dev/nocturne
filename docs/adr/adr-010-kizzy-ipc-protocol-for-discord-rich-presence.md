# ADR-010: Adopt Kizzy IPC Protocol for Local Discord Rich Presence

## Status
Accepted

## Context
Broadcasting music status to Discord on Android cannot use desktop Unix sockets.

## Decision
Integrate the Kizzy IPC Android Service bridge to communicate directly with Discord Android client over local IPC.

## Consequences
Enables native "Listening to Nocturne" presence on Discord mobile without requiring desktop companion apps.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
