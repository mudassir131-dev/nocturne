# ADR-002: Standardize on AndroidX Media3 ExoPlayer for Playback Engine

## Status
Accepted

## Context
Legacy MediaPlayer lacked robust adaptive streaming, chunk caching, custom DataSource injection, and modern foreground lifecycle management.

## Decision
Adopt AndroidX Media3 ExoPlayer as the core playback and media session engine across Nocturne.

## Consequences
Provides state-of-the-art chunk streaming, gapless playback, lockscreen integration, and seamless Android Auto compatibility.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
