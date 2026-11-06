# Biquad Peaking Equalizer Filter Coefficient Calculation Formulas

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **biquad peaking equalizer filter coefficient calculation formulas** within the Nocturne application.

## Key Principles & Invariants
- Standard Robert Bristow-Johnson (RBJ) Audio EQ Cookbook peaking filter equations.
- Intermediate variables: omega = 2 * PI * f0 / Fs, alpha = sin(omega) / (2 * Q), A = 10^(gainDB / 40).
- Coefficients: b0 = 1 + alpha * A, b1 = -2 * cos(omega), b2 = 1 - alpha * A, a0 = 1 + alpha / A, a1 = -2 * cos(omega), a2 = 1 - alpha / A.
- Normalization step: dividing all coefficients by a0 to enable Direct Form II computation.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
