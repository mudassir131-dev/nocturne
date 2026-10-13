# ADR-015: Enforce 32-Bit Float PCM Internal Audio Processing Pipeline

## Status
Accepted

## Context
16-bit integer PCM audio processing introduces quantization noise and digital clipping when applying positive equalizer gain.

## Decision
Standardize the entire native C++ DSP pipeline on IEEE 754 32-bit floating-point audio samples.

## Consequences
Provides +700dB of internal headroom, eliminating digital clipping before the final limiter stage.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
