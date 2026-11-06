# Binaural Downmixing Matrix Coefficients for 5.1 Multichannel Audio

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **binaural downmixing matrix coefficients for 5.1 multichannel audio** within the Nocturne application.

## Key Principles & Invariants
- ITU-R downmix matrix coefficients for Left, Right, Center, LFE, and Surround channels.
- Binaural phase alignment preventing comb filtering and center-channel frequency cancellation.
- Virtual surround spatialization for standard stereo wired and wireless headphones.
- Low-complexity floating-point SIMD matrix vector multiplication.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
