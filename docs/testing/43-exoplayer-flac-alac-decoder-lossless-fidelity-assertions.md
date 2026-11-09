# ExoPlayer FLAC and ALAC Decoder Lossless Fidelity Assertions

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **exoplayer flac alac decoder lossless fidelity assertions** within the Nocturne application.

## Key Principles & Invariants
- End-to-end bit-exact output verification comparing decoded PCM samples against reference uncompressed WAV data.
- Validation across 16-bit, 24-bit, and 32-bit floating point audio streams up to 192kHz sampling rates.
- Automated checksum hashing (SHA-256) of decoded output audio buffers confirming zero compression artifacts.
- Channel mapping assertions verifying correct 5.1 and 7.1 surround channel routing to stereo audio sinks.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.testing`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
