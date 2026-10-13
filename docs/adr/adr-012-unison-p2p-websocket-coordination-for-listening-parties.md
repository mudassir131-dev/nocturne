# ADR-012: Adopt Lightweight WebSocket Protocol for Unison Listening Parties

## Status
Accepted

## Context
Synchronizing music playback across friends in real time requires low latency and minimal server overhead.

## Decision
Implement the Unison collaborative listening protocol over secure WebSockets with JSON state payloads and clock offset calculations.

## Consequences
Keeps listening party members synchronized within 50ms without heavy infrastructure.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
