# Marquee Text Crossfade Edge Mask and SubcomposeLayout

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **marquee text crossfade edge mask and subcompose layout** within the Nocturne application.

## Key Principles & Invariants
- SubcomposeLayout measuring song title text width against available container boundaries before initiating scroll.
- Dynamic horizontal scrolling animation with configurable start delay (1,500ms) and repeat pause duration.
- Linear alpha gradient edge masks applied via graphicsLayer and BlendMode.DstIn to create soft fading text borders.
- Automatic scroll halting when audio playback is paused or when text composable scrolls out of visible viewport.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
