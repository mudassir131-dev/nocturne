# ADR-013: Adopt Decorrelated Jitter Retry Policy for Media Chunk Downloads

## Status
Accepted

## Context
Fixed network retry schedules caused concurrent audio chunk downloads to synchronize during cellular handover.

## Decision
Incorporate decorrelated jitter exponential backoff for media stream chunk fetch retries.

## Consequences
Smooths out retry spikes and improves chunk fetch recovery on congested mobile networks.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
