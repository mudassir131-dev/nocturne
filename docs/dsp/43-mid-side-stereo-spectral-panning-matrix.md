# Mid Side Stereo Spectral Panning Matrix

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **mid side stereo spectral panning matrix** within the Nocturne application.

## Key Principles & Invariants
- Mid/Side encoder matrix: M = (L + R) / sqrt(2), S = (L - R) / sqrt(2).
- Independent frequency-domain stereo width enhancement: expands side channel above 400Hz while keeping bass (< 120Hz) strictly centered mono.
- Phase correlation meter asserting correlation >= 0.0 to guarantee monophonic compatibility without cancellation.
- Reversible decoder matrix reconstructing discrete L and R stereo channels without spectral coloration.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
