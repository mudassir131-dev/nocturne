# AudioSession AudioEffect Priority Chain and Insertion Order

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audiosession audioeffect priority chain and insertion order** within the Nocturne application.

## Key Principles & Invariants
- Mandates strict filter order: Biquad Parametric EQ -> Bass Boost -> Virtualizer -> Dynamics Compressor -> Soft Limiter.
- Allocates unique audioSessionId linked directly to the primary ExoPlayer AudioTrack instance.
- Gracefully handles AudioEffect initialization failures on fragmented vendor OEM ROMs.
- Real-time bypass switches allowing instant effect toggling without pipeline reset or audio dropouts.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
