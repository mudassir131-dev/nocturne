# De-Esser High Frequency Sibilance Spectral Compressor

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **de esser high frequency sibilance spectral compressor** within the Nocturne application.

## Key Principles & Invariants
- Band-pass sidechain filter centered between 5kHz and 9kHz detecting vocal sibilance ("s", "sh", "t" sounds).
- Fast dynamic gain reduction (1ms attack, 20ms release) targeting isolated sibilant frequency bursts.
- Split-band processing mode compressing only the offending sibilant band while leaving lower frequencies untouched.
- Threshold and reduction amount controls providing surgical vocal harshness reduction on compressed streaming audio.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
