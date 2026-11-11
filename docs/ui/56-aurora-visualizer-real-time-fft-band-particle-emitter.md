# Aurora Visualizer Real Time FFT Band Particle Emitter

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **aurora visualizer real time fft band particle emitter** within the Nocturne application.

## Key Principles & Invariants
- Canvas-based particle rendering system driven by 64-band floating-point FFT spectrum magnitudes.
- Bass band energy modulating particle explosion velocity and luminescence intensity in real time.
- AGSL (Android Graphics Shading Language) custom shaders offloading color gradient blend math to device GPU.
- Strict 60 fps / 120 fps target frame pacing maintaining low CPU overhead (< 4% utilization) during playback.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.ui`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
