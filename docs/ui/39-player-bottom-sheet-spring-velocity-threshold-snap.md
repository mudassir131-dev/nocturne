# Player Bottom Sheet Spring Velocity Threshold Snap

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **player bottom sheet spring velocity threshold snap** within the Nocturne application.

## Key Principles & Invariants
- Custom bottom sheet state machine with DampedHarmonicOscillator spring physics for natural touch gestures.
- Fling velocity detection snapping to expanded player or mini-player state based on flick speed and drag distance.
- Seamless interpolation of corner radius, padding, album art size, and control opacity between docked and expanded states.
- Edge-to-edge window inset integration adjusting bottom sheet anchor to navigation bar height.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
