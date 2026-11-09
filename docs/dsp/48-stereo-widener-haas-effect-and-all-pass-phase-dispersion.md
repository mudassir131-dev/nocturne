# Stereo Widener Haas Effect and All Pass Phase Dispersion

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **stereo widener haas effect and all pass phase dispersion** within the Nocturne application.

## Key Principles & Invariants
- Micro-delay delay line (5ms to 25ms) creating spatial Haas psychoacoustic stereo width enhancement.
- First-order all-pass filter network dispersing frequency phase response without altering frequency amplitude.
- Mono compatibility safety check measuring cross-correlation coefficient and attenuating out-of-phase energy.
- Dry/wet blend parameter providing continuous control from subtle widening to wide ambient acoustic stage.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
