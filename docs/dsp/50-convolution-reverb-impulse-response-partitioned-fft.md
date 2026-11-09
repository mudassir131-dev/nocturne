# Convolution Reverb Impulse Response Partitioned FFT

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **convolution reverb impulse response partitioned fft** within the Nocturne application.

## Key Principles & Invariants
- Uniformly partitioned block convolution algorithm computing FFT-based finite impulse response (FIR) filtering.
- Zero-latency direct-form FIR convolution for initial 128 samples followed by frequency-domain block processing.
- Support for loading standard stereo WAV impulse responses up to 5 seconds in duration.
- ARM NEON SIMD vectorization computing complex multiplication and inverse FFT in sub-millisecond frames.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
