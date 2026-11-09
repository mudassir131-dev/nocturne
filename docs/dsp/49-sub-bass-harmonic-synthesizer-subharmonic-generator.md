# Sub-Bass Harmonic Synthesizer Subharmonic Generator

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **sub bass harmonic synthesizer subharmonic generator** within the Nocturne application.

## Key Principles & Invariants
- Frequency tracking phase-locked loop (PLL) isolating fundamental low frequencies between 40Hz and 120Hz.
- Subharmonic generator synthesizing lower octave signals (f/2) to enrich bass depth on capable subwoofer setups.
- Psychoacoustic harmonic generator creating upper harmonics (2f, 3f) enabling bass perception on mobile speakers.
- Low-pass crossover isolation confining synthetic harmonics strictly below 150Hz.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
