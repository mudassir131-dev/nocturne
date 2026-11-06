# Triangular Probability Density Function Dither and Noise Shaping Filter

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **triangular probability density function dither and noise shaping filter** within the Nocturne application.

## Key Principles & Invariants
- Generation of TPDF dither by summing two independent uniform random variables.
- Error feedback loop (noise shaping) subtracting filtered quantization error from subsequent samples.
- Complete elimination of quantization distortion and harmonic correlation at low amplitudes.
- Mathematically proven constant noise floor power across all input signal levels.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
