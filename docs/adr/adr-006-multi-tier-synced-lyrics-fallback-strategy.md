# ADR-006: Adopt Multi-Tier Lyrics Fallback Strategy Across LRCLIB and Kugou

## Status
Accepted

## Context
No single lyrics database provides universal coverage across Western, Asian, and indie tracks.

## Decision
Chain lyrics providers in a tiered fallback: query LRCLIB for open-source line/word sync, fall back to Kugou for Asian catalog, then Paxsenix.

## Consequences
Achieves industry-leading lyrics coverage while maintaining sub-second lyrics fetch times.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
