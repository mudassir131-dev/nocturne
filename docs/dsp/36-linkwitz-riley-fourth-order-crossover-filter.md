# Linkwitz-Riley Fourth-Order Crossover Filter

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **linkwitz riley fourth order crossover filter** within the Nocturne application.

## Key Principles & Invariants
- Cascaded Butterworth second-order filters generating 24 dB/octave attenuation slope (LR4).
- Zero phase difference across crossover frequency yielding flat combined magnitude response with 0dB summing gain.
- Divides input audio stream into low-frequency (< 150Hz) and mid-high frequency bands for split multiband processing.
- Vectorized ARM NEON implementation processing stereo float32 samples in 4-sample parallel vectors.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
