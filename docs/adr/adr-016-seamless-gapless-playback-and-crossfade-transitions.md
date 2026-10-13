# ADR-016: Adopt Dual ExoPlayer Instance Buffer Preloading for Crossfade Transitions

## Status
Accepted

## Context
A single ExoPlayer instance cannot decode and render two independent media sources simultaneously with overlapping gain fades.

## Decision
Instantiate a secondary background player instance preloading the upcoming track and crossfade volumes linearly across 1-10 seconds.

## Consequences
Delivers club-quality studio crossfading without clicks, pops, or buffering stalls.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
