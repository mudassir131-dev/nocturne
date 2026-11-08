# Marquee Overflow Text Composables Subcompose Layout

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **marquee overflow text composables subcompose layout** within the Nocturne application.

## Key Principles & Invariants
- SubcomposeLayout measuring text intrinsic width against available container bounds to trigger marquee only on overflow.
- Continuous linear scroll animation with 2-second initial pause and 1-second pause at loop boundary.
- Soft horizontal fade edge mask using linear gradient alpha brush preventing harsh text clipping at container edges.
- Low-power mode detector pausing marquee scrolling when device battery saver is active or screen is off.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
