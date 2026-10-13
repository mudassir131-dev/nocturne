# ADR-005: Adopt Room Persistence with SQLite Write-Ahead Logging (WAL)

## Status
Accepted

## Context
Simultaneous database reads from UI ViewModels and bulk writes from background playlist imports caused SQLite database locks and UI stutter.

## Decision
Configure Room Database to operate in SQLite Write-Ahead Logging (WAL) journal mode with asynchronous transaction dispatchers.

## Consequences
Allows non-blocking concurrent reads during high-volume import writes, ensuring 60fps UI smoothness.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
