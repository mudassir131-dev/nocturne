# Linear Phase FIR Convolution Overlap Save Engine

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **linear phase fir convolution overlap save engine** within the Nocturne application.

## Key Principles & Invariants
- Partitioned frequency-domain convolution engine using the Overlap-Save algorithm for arbitrary FIR filter lengths.
- Zero phase distortion guaranteeing symmetrical impulse response and identical group delay across all audio frequencies.
- Supports custom speaker correction and headphone EQ impulse response (IR) WAV files up to 4096 taps.
- Background convolution worker operating in lock-free ring buffer architecture preventing audio thread glitches.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
