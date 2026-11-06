# Spotify Canvas H.264 MP4 Loop Playback and Seamless Looping Spec

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **spotify canvas h.264 mp4 loop playback and seamless looping spec** within the Nocturne application.

## Key Principles & Invariants
- Video codec: H.264 / AVC Baseline Profile Level 3.1 at 720x1280 resolution.
- Looping behavior: REPEAT_MODE_ALL with zero-buffer transition between loop ends.
- Hardware video decoder instance separation avoiding audio decoder resource contention.
- Muted audio track stripping preventing secondary audio sink initialization.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.specs`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
