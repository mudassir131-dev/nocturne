# AudioTrack Offload Sleep Mode and DSP Power Profiling

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **audiotrack offload sleep mode and dsp power profiling** within the Nocturne application.

## Key Principles & Invariants
- AudioTrack offload mode configuration passing compressed AAC/FLAC bitstreams directly to hardware DSP.
- Application processor sleep state enablement during offload playback reducing active CPU wakeups to once per 30s.
- Dynamic fallback to software decoding pipeline upon activation of custom EQ, virtualizer, or pitch shifting.
- Power profiling metrics asserting significant battery longevity gains on supported Qualcomm and Tensor chips.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
