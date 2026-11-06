# Virtualizer Spatial Binaural Impulse Response Matrix Specification

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **virtualizer spatial binaural impulse response matrix specification** within the Nocturne application.

## Key Principles & Invariants
- Head-related transfer function (HRTF) cross-feed matrix with frequency-dependent interaural delay.
- Interaural time difference (ITD) calibration between 0.2ms and 0.7ms for horizontal plane realism.
- Interaural level difference (ILD) high-frequency attenuation simulating acoustic head shadow.
- Toggleable strength parameter (0% to 100%) with linear perceptual spatial widening.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
