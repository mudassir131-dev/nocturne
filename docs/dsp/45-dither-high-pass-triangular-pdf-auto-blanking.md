# Dither High Pass Triangular PDF Auto Blanking

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **dither high pass triangular pdf auto blanking** within the Nocturne application.

## Key Principles & Invariants
- Generates High-Pass Triangular Probability Density Function (HP-TPDF) dither noise for 32-bit float to 16/24-bit PCM quantization.
- High-pass spectral shaping pushes dither noise power into high frequencies (> 18kHz) where human hearing is least sensitive.
- Auto-blanking detector: immediately mutes dither noise generator when audio stream contains digital silence (> 50ms at -96dBFS).
- Prevents background hiss and noise floor buildup during track transitions and player pauses.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.dsp`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
