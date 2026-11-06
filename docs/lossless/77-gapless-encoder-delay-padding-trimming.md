# Gapless Encoder Delay and Padding Trimming for Lossless Streams

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **gapless encoder delay and padding trimming for lossless streams** within the Nocturne application.

## Key Principles & Invariants
- Accurate extraction of iTunSMPB metadata and LAME encoder delay/padding samples.
- Sample-accurate audio frame trimming at track beginning and end boundaries.
- Seamless transition without clicks, pops, or silence between contiguous album tracks.
- Support for both FLAC and ALAC gapless playback mechanisms.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.lossless`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
