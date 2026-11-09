# Parametric EQ Bell Shelf Notch Biquad Coefficient Math

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **parametric eq bell shelf notch biquad coefficient math** within the Nocturne application.

## Key Principles & Invariants
- Direct Form II Transposed biquad filter implementation computing Robert Bristow-Johnson Audio EQ Cookbook formulas.
- Support for peaking bell, low-shelf, high-shelf, band-pass, and notch filter topologies with variable Q-factors.
- Real-time coefficient smoothing using one-pole low-pass interpolation to eliminate zipper noise during slider movement.
- Double-precision intermediate floating-point calculation preventing limit cycles and quantization noise.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
