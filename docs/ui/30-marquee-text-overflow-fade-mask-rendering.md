# Marquee Text Overflow Gradient Alpha Masking and Idle Timing

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **marquee text overflow gradient alpha masking and idle timing** within the Nocturne application.

## Key Principles & Invariants
- Scrolls overflowing track titles and artist names smoothly using basicMarquee().
- Adds subtle horizontal alpha gradient mask to edges to prevent jarring hard text cutoff.
- Pauses marquee animation for 2,000 ms at starting edge before initiating continuous scroll.
- Automatically stops marquee when player is paused or screen is locked to conserve GPU power.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
