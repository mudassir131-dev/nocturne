# Harmonic Distortion Analogue Tube Warmth Emulation Curve

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **harmonic distortion analogue tube warmth emulation curve** within the Nocturne application.

## Key Principles & Invariants
- Non-linear transfer function introducing controlled even-order harmonic excitation (2nd and 4th harmonics).
- Asymmetric soft-clipping polynomial curve emulating triode vacuum tube saturation dynamics.
- Oversampled processing stage preventing digital foldback aliasing in upper audible spectrum.
- Dynamic drive parameter scaling compensating for input gain variations across track loudness levels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
