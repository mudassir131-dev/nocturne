# Media3 Forward Backward Scrubbing Smoothing Filter

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **media3 forward backward scrubbing smoothing filter** within the Nocturne application.

## Key Principles & Invariants
- Debounced seek request aggregator collapsing rapid touch slider movements into discrete seek commands.
- AudioTrack soft mute ramp (15ms linear gain reduction) preceding seek position jumps to eliminate pops.
- Fast keyframe seeking during active gesture followed by exact sample positioning upon finger release.
- Smooth waveform preview position feedback synchronized with gesture touch coordinates.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.playback`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
