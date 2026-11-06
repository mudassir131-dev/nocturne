# Biquad Notch Rejection Filter Transfer Function and Coefficient Math

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **biquad notch rejection filter transfer function and coefficient math** within the Nocturne application.

## Key Principles & Invariants
- Narrow-band notch rejection filter eliminating 50Hz/60Hz AC electrical hum and line noise.
- Coefficients: b0 = 1, b1 = -2 * cos(omega), b2 = 1, a0 = 1 + alpha, a1 = -2 * cos(omega), a2 = 1 - alpha.
- Deep attenuation (>60dB) at notch center frequency with minimal attenuation in neighboring bands.
- Zero ripple outside the immediate notch rejection bandwidth.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
