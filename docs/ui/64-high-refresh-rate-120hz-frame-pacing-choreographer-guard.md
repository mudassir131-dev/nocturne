# High Refresh Rate 120Hz Frame Pacing Choreographer Guard

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **high refresh rate 120hz frame pacing choreographer guard** within the Nocturne application.

## Key Principles & Invariants
- Choreographer frame callback integration synchronizing visualizer and lyrics rendering with display VSYNC.
- Dynamic support for variable refresh rate displays (60 Hz, 90 Hz, 120 Hz) without animation speed distortion.
- Frame budget monitor dropping secondary visual effects if render phase exceeds 7.5 milliseconds (at 120 Hz).
- Battery-saving thermal throttler scaling visualizer refresh down to 60 Hz if device thermal status reaches SEVERE.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
