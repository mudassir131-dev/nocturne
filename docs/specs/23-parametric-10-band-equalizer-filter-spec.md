# 10-Band Parametric Equalizer Center Frequencies and Q-Factor Curves

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **10-band parametric equalizer center frequencies and q-factor curves** within the Nocturne application.

## Key Principles & Invariants
- Fixed center frequencies: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.
- Gain adjustment range: -12.0dB to +12.0dB with 0.1dB step resolution.
- Constant quality factor (Q = 1.414) maintaining symmetrical half-power bandwidth.
- Coefficient clamping preventing digital filter instability across boundary conditions.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
