# Compose Gestures Vertical Drag to Dismiss Player Animation

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **compose gestures vertical drag to dismiss player animation** within the Nocturne application.

## Key Principles & Invariants
- Interactive pointerInput gesture modifier handling vertical swipe-down transitions on full-screen player sheet.
- Dynamic scale and alpha interpolation transforming player container into mini-player bottom bar smoothly.
- VelocityTracker integration detecting high-speed fling gestures snapping player state regardless of drag distance.
- Invariant ensuring nested scrolling in lyrics or queue lists does not trigger accidental sheet dismissal.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
