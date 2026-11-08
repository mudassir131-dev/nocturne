# Equalizer Graphic Curve Touch Interactive Canvas Drag

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **equalizer graphic curve touch interactive canvas drag** within the Nocturne application.

## Key Principles & Invariants
- Interactive Jetpack Compose Canvas rendering continuous frequency response curve using Catmull-Rom splines.
- Multi-touch gesture detector allowing drag-to-adjust on individual band control points with visual glow rings.
- Real-time dB readout tooltip floating above dragged slider node showing exact gain adjustment (-12.0dB to +12.0dB).
- Smooth spring animation snapping node back to 0.0dB baseline upon double-tap reset gesture.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
