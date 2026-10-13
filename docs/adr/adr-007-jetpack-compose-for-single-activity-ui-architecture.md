# ADR-007: Adopt 100% Declarative Jetpack Compose for Single-Activity Architecture

## Status
Accepted

## Context
Traditional Android XML layouts and Fragment lifecycles resulted in complex state bugs, boilerplate binding code, and difficult theme transitions.

## Decision
Rebuild the entire Nocturne UI using pure Jetpack Compose under a single MainActivity host.

## Consequences
Dramatically reduces codebase size, simplifies state management via Compose State and Flow, and enables fluid animations.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
