# Bluetooth Latency Compensation and AV Sync Offset

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **bluetooth latency compensation and av sync offset** within the Nocturne application.

## Key Principles & Invariants
- Dynamic Bluetooth latency calculation querying AudioTrack.getPlaybackHeadPosition() and output device latency profiles.
- Automatic offset compensation aligning time-synchronized lyrics and audio visualizers with acoustic output.
- Codec-specific latency presets compensating for AAC (120ms), SBC (180ms), and LDAC/aptX Adaptive (40-80ms) transmission buffers.
- Real-time adjustment responding dynamically when user connects or disconnects external Bluetooth speakers or earbuds.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
