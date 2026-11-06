# Second Order Butterworth High Pass Filter Coefficient Formulas

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **second order butterworth high pass filter coefficient formulas** within the Nocturne application.

## Key Principles & Invariants
- Second-order Butterworth high-pass filter eliminating sub-audible DC offset and rumble (<20Hz).
- Coefficients: b0 = (1 + cos(omega)) / 2, b1 = -(1 + cos(omega)), b2 = (1 + cos(omega)) / 2.
- Protection of mobile micro-speakers against over-excursion caused by extreme sub-bass signals.
- Numerical stability verified under single-precision float32 execution.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
