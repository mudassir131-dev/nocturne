# Dithered Quantization and Noise Shaping for 24-Bit Downsampling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dithered quantization and noise shaping for 24-bit downsampling** within the Nocturne application.

## Key Principles & Invariants
- Triangular probability density function (TPDF) dither addition before bit-depth reduction.
- Modified Lipshitz noise shaping filter pushing quantization noise into psychoacoustically inaudible bands (>18kHz).
- Preservation of low-level harmonic detail in 24-bit to 16-bit DAC conversions.
- Full compatibility with high-resolution USB DAC hardware.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
