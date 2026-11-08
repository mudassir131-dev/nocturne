# Aurora Canvas Multi-Pass Gaussian Blur Shader

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **aurora canvas multi pass gaussian blur shader** within the Nocturne application.

## Key Principles & Invariants
- Two-pass separable Gaussian blur (horizontal pass followed by vertical pass) implemented with AGSL on Android 13+.
- Reduces computational complexity from O(N^2) to O(2N) enabling smooth 60/120 FPS backdrop animations.
- Downsamples album artwork bitmap by 4x before shader pass to minimize fill-rate pressure on mobile GPUs.
- Dynamic color blooming effect blending dominant album art hues with a dark gradient scrim for text readability.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
